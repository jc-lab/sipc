package server

import (
	"crypto/rand"
	"errors"
	"github.com/flynn/noise"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"github.com/jc-lab/sipc/go/transport"
	"github.com/jc-lab/sipc/go/util"
	"golang.org/x/crypto/curve25519"
	"google.golang.org/protobuf/proto"
	"log"
	"net"
	"sync"
	"time"
)

type SipcServer struct {
	handshakeTimeout time.Duration
	allowReconnect   bool
	disablePidCheck  bool
	transport        transport.Transport
	listener         net.Listener
	localStaticKey   noise.DHKey
	childMap         map[string]*SipcChild
	mutex            sync.RWMutex
}

type SipcServerConfig struct {
	Path             string
	LocalPrivateKey  []byte
	HandshakeTimeout time.Duration
	Transport        transport.Transport
	AllowReconnect   bool
	DisablePidCheck  bool
}

func NewSipcServer(config SipcServerConfig) (*SipcServer, error) {
	server := &SipcServer{
		handshakeTimeout: config.HandshakeTimeout,
		allowReconnect:   config.AllowReconnect,
		transport:        config.Transport,

		mutex:    sync.RWMutex{},
		childMap: make(map[string]*SipcChild),
	}
	if server.handshakeTimeout == 0 {
		server.handshakeTimeout = time.Minute * 1
	}
	if server.transport == nil {
		t, err := transport.NewTransport(transport.GetDefaultTransportType())
		if err != nil {
			return nil, err
		}
		server.transport = t
	}

	server.disablePidCheck = config.DisablePidCheck

	if config.LocalPrivateKey != nil {
		pubkey, err := curve25519.X25519(config.LocalPrivateKey, curve25519.Basepoint)
		if err != nil {
			return nil, err
		}
		server.localStaticKey = noise.DHKey{Private: config.LocalPrivateKey, Public: pubkey}
	} else {
		localStaticKey, err := noise.DH25519.GenerateKeypair(rand.Reader)
		if err != nil {
			return nil, err
		}
		server.localStaticKey = localStaticKey
	}

	listener, err := server.transport.Listen(server.transport.NewDefaultPath())
	if err != nil {
		return nil, err
	}

	server.listener = listener
	go server.listenWorker()

	return server, nil
}

func (server *SipcServer) Shutdown() {
	server.mutex.Lock()
	defer server.mutex.Unlock()
	if server.listener != nil {
		_ = server.listener.Close()
		server.listener = nil
	}
}

func (server *SipcServer) listenWorker() {
	for {
		server.mutex.RLock()
		listener := server.listener
		server.mutex.RUnlock()

		if listener == nil {
			break
		}

		conn, err := listener.Accept()
		if err != nil {
			log.Println(err)
			break
		}
		server.childHandshake(conn)
	}
}

func (server *SipcServer) childHandshake(conn net.Conn) {
	handshaked := false
	defer func() {
		if !handshaked {
			_ = conn.Close()
		}
	}()

	peerCred, err := server.transport.GetPeerCredentials(conn)
	if err != nil {
		log.Println(err)
		return
	}

	cs := noise.NewCipherSuite(noise.DH25519, noise.CipherChaChaPoly, noise.HashSHA256)
	handshakeState, err := noise.NewHandshakeState(noise.Config{
		CipherSuite:   cs,
		Random:        rand.Reader,
		Pattern:       noise.HandshakeXK,
		Initiator:     false,
		StaticKeypair: server.localStaticKey,
	})
	if err != nil {
		log.Println(err)
		return
	}

	var sipcClient *SipcChild = nil

	handleError := func(err error) bool {
		if err != nil {
			if sipcClient != nil {
				sipcClient.handshakeFailure(err)
			} else {
				log.Println(err)
			}
			return true
		}
		return false
	}

	handshakeResultCheck := func(payload []byte, cs1 *noise.CipherState, cs2 *noise.CipherState, err error) ([]byte, bool) {
		if handleError(err) {
			return nil, true
		}
		if cs1 != nil || cs2 != nil {
			if sipcClient != nil {
				if sipcClient.handshakeSuccess(conn, cs1, cs2) {
					handshaked = true
				} else {
					_ = conn.Close()
				}
			}
			return payload, true
		}
		return payload, false
	}

	for {
		readBuf, err := util.ReadPacket(conn)
		if handleError(err) {
			break
		}
		if len(readBuf) <= 0 {
			handleError(errors.New("connection closed"))
		}
		payload, complete := handshakeResultCheck(handshakeState.ReadMessage(nil, readBuf))
		if complete {
			break
		}

		if payload != nil {
			clientHello := &sipc_proto.ClientHelloPayload{}
			err = proto.Unmarshal(payload, clientHello)
			if handleError(err) {
				break
			}

			if sipcClient != nil {
				handleError(errors.New("invalid state"))
				break
			}

			sipcClient = server.getChild(clientHello.ConnectionId)
			if sipcClient == nil {
				handleError(errors.New("invalid connection id"))
				break
			}

			if !server.disablePidCheck {
				pidPointer, ok := sipcClient.pidPromise.Wait(server.handshakeTimeout)
				if ok {
					if *pidPointer != peerCred.Pid {
						handleError(errors.New("not matched peer pid"))
						break
					}
				} else {
					handleError(errors.New("timeout"))
				}
			}
		}

		payload, complete = handshakeResultCheck(handshakeState.WriteMessage(nil, nil))
		if payload != nil {
			err = util.WritePacket(conn, payload)
			if handleError(err) {
				break
			}
		}
		if complete {
			break
		}
	}
}

func (server *SipcServer) addChild(connectionId string, child *SipcChild) {
	server.mutex.Lock()
	defer server.mutex.Unlock()
	server.childMap[connectionId] = child
}

func (server *SipcServer) getChild(connectionId string) *SipcChild {
	server.mutex.RLock()
	defer server.mutex.RUnlock()
	return server.childMap[connectionId]
}

func (server *SipcServer) removeChild(connectionId string) {
	server.mutex.Lock()
	defer server.mutex.Unlock()
	delete(server.childMap, connectionId)
}
