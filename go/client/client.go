package client

import (
	"bytes"
	"crypto/rand"
	"encoding/base64"
	"errors"
	"github.com/flynn/noise"
	"github.com/jc-lab/sipc/go/sipc_error"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"github.com/jc-lab/sipc/go/transport"
	"github.com/jc-lab/sipc/go/util"
	"google.golang.org/protobuf/proto"
	"io"
	"net"
	"time"
)

type State int

const (
	kStateIdle State = 1 + iota
	kStateHandshake
	kStateEstablished
	kStateClosed
)

type SipcClient struct {
	io.ReadWriteCloser

	connectInfo sipc_proto.ConnectInfo

	HandshakeTimeout time.Duration

	localStaticKey noise.DHKey
	state          State
	transport      transport.Transport
	connection     net.Conn
	handshakeState *noise.HandshakeState
	handshakeCh    chan error

	csServer   *noise.CipherState
	csClient   *noise.CipherState
	readBuffer *bytes.Buffer
}

func NewSipcClient(connectInfo string) (*SipcClient, error) {
	return NewSipcClientWithAllowRemote(connectInfo, false)
}

func NewSipcClientWithAllowRemote(connectInfo string, allowRemote bool) (*SipcClient, error) {
	client := &SipcClient{
		HandshakeTimeout: time.Second * 60,
		state:            kStateIdle,
		readBuffer:       bytes.NewBuffer(make([]byte, 1024)),
	}

	localStaticKey, err := noise.DH25519.GenerateKeypair(rand.Reader)
	if err != nil {
		return nil, err
	}
	client.localStaticKey = localStaticKey

	connectInfoBinary, err := base64.URLEncoding.DecodeString(connectInfo)
	if err != nil {
		return nil, err
	}
	err = proto.Unmarshal(connectInfoBinary, &client.connectInfo)
	if err != nil {
		return nil, err
	}

	if client.connectInfo.TransportType == sipc_proto.TransportType_kTcp {
		if !allowRemote {
			return nil, errors.New("remote connection not allowed")
		}
	}

	return client, nil
}

func (client *SipcClient) IsConnected() bool {
	return client.state == kStateEstablished
}

func (client *SipcClient) IsClosed() bool {
	return client.state == kStateClosed
}

func (client *SipcClient) Start() error {
	var err error

	if client.transport == nil {
		var t transport.Transport

		if client.connectInfo.TransportType == sipc_proto.TransportType_kTcp {
			t = transport.NewTcpTransport()
		} else {
			t, err = transport.NewTransport(client.connectInfo.TransportType)
			if err != nil {
				return err
			}
		}
		client.transport = t
	}

	client.handshakeCh = make(chan error, 1)
	conn, err := client.transport.Connect(client.connectInfo.TransportAddress)
	if err != nil {
		return err
	}
	client.state = kStateHandshake
	client.connection = conn

	cs := noise.NewCipherSuite(noise.DH25519, noise.CipherChaChaPoly, noise.HashSHA256)
	//localKeyPair, err := cs.GenerateKeypair(rand.Reader)
	//if err != nil {
	//	return err
	//}
	handshakeState, err := noise.NewHandshakeState(noise.Config{
		CipherSuite:   cs,
		Random:        rand.Reader,
		Pattern:       noise.HandshakeXK,
		Initiator:     true,
		StaticKeypair: client.localStaticKey,
		PeerStatic:    client.connectInfo.PublicKey,
	})
	if err != nil {
		return err
	}
	client.handshakeState = handshakeState

	clientHello := &sipc_proto.ClientHelloPayload{
		ConnectionId: client.connectInfo.ConnectionId,
	}

	handshakeResultCheck := func(payload []byte, cs1 *noise.CipherState, cs2 *noise.CipherState, err error) ([]byte, int) {
		if err != nil {
			client.handshakeCh <- err
			return nil, -1
		}
		if cs1 != nil || cs2 != nil {
			client.csServer = cs2
			client.csClient = cs1
			client.state = kStateEstablished
			client.readBuffer.Reset()
			client.handshakeCh <- nil
			return payload, 1
		}
		return payload, 0
	}

	go func() {
		writeBuf, err := proto.Marshal(clientHello)
		if err != nil {
			client.handshakeCh <- err
			return
		}
		for {
			payload, cs1, cs2, err := handshakeState.WriteMessage(nil, writeBuf)
			writeBuf = nil
			if err == nil && payload != nil {
				err = util.WritePacket(conn, payload)
			}
			payload, complete := handshakeResultCheck(payload, cs1, cs2, err)
			if complete != 0 {
				break
			}

			readBuf, err := util.ReadPacket(conn)
			if err != nil {
				client.handshakeCh <- err
				break
			}
			if len(readBuf) <= 0 {
				client.handshakeCh <- errors.New("connection closed")
				break
			}

			payload, complete = handshakeResultCheck(handshakeState.ReadMessage(nil, readBuf))
			if complete != 0 {
				break
			}
		}
	}()

	select {
	case res := <-client.handshakeCh:
		return res
	case <-time.After(client.HandshakeTimeout):
		return sipc_error.HANDSHAKE_TIMEOUT
	}
}

func (client *SipcClient) Read(p []byte) (int, error) {
	if client.state != kStateEstablished {
		return -1, sipc_error.NOT_CONNECTED
	}

	n, err := util.BufferedRead(client.readBuffer, p, func() ([]byte, error) {
		buf, err := util.ReadPacket(client.connection)
		if err != nil {
			return nil, err
		}
		return client.csServer.Decrypt(nil, nil, buf)
	})
	if err != nil {
		client.onInactive(err)
	}
	return n, err
}

func (client *SipcClient) Write(p []byte) (n int, err error) {
	if client.state != kStateEstablished {
		return -1, sipc_error.NOT_CONNECTED
	}

	ct, err := client.csClient.Encrypt(nil, nil, p)
	if err != nil {
		return 0, err
	}

	err = util.WritePacket(client.connection, ct)
	if err != nil {
		return 0, err
	}

	return len(p), nil
}

func (client *SipcClient) closeObject() {
	client.state = kStateClosed
}

func (client *SipcClient) Close() error {
	if client.state != kStateEstablished {
		return sipc_error.NOT_CONNECTED
	}

	client.closeObject()

	return client.connection.Close()
}

func (client *SipcClient) onInactive(cause error) {
	client.connection = nil
	client.csServer = nil
	client.csClient = nil
	if client.connectInfo.AllowReconnect {
		client.state = kStateIdle
		_ = client.Start()
	} else {
		client.closeObject()
	}
}
