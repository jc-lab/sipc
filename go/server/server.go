package server

import (
	"crypto/rand"
	"errors"
	"github.com/flynn/noise"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"github.com/jc-lab/sipc/go/transport"
	"github.com/jc-lab/sipc/go/util"
	"google.golang.org/protobuf/proto"
	"log"
	"net"
	"time"
)

type SipcServer struct {
	handshakeTimeout time.Duration
	transport        transport.Transport
	listener         net.Listener
	localStaticKey   noise.DHKey
	childMap         map[string]*SipcChild
}

type SipcServerConfig struct {
	Path             string
	LocalPrivateKey  []byte
	HandshakeTimeout time.Duration
	Transport        transport.Transport
}

func NewSipcServer(config SipcServerConfig) (*SipcServer, error) {
	server := &SipcServer{
		handshakeTimeout: config.HandshakeTimeout,
		transport:        config.Transport,

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

	localStaticKey, err := noise.DH25519.GenerateKeypair(rand.Reader)
	if err != nil {
		return nil, err
	}
	server.localStaticKey = localStaticKey

	listener, err := server.transport.Listen(server.transport.NewDefaultPath())
	if err != nil {
		return nil, err
	}

	server.listener = listener
	go server.listenWorker()

	return server, nil
}

func (server *SipcServer) Shutdown() {
	if server.listener != nil {
		_ = server.listener.Close()
		server.listener = nil
	}
}

func (server *SipcServer) listenWorker() {
	for {
		conn, err := server.listener.Accept()
		if err != nil {
			println(err)
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
		Pattern:       noise.HandshakeNX,
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
				handshaked = true
				sipcClient.handshakeSuccess(conn, cs1, cs2)
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

			sipcClient = server.childMap[clientHello.ConnectionId]
			if sipcClient == nil {
				handleError(errors.New("invalid connection id"))
				break
			}

			if sipcClient.pid != peerCred.Pid {
				handleError(errors.New("not matched peer pid"))
				break
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
