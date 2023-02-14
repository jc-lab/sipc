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

	state          State
	transport      transport.Transport
	connection     net.Conn
	handshakeState *noise.HandshakeState
	handshakeCh    chan error

	cs1        *noise.CipherState
	cs2        *noise.CipherState
	readBuffer *bytes.Buffer
}

func NewSipcClient(connectInfo string) (*SipcClient, error) {
	client := &SipcClient{
		HandshakeTimeout: time.Second * 60,
		state:            kStateIdle,
	}
	connectInfoBinary, err := base64.StdEncoding.DecodeString(connectInfo)
	if err != nil {
		return nil, err
	}
	err = proto.Unmarshal(connectInfoBinary, &client.connectInfo)
	if err != nil {
		return nil, err
	}

	return client, nil
}

func (client *SipcClient) Start() error {
	t, err := transport.NewTransport(client.connectInfo.TransportType)
	if err != nil {
		return err
	}
	client.transport = t
	client.handshakeCh = make(chan error, 1)
	conn, err := t.Connect(client.connectInfo.TransportAddress)
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
		CipherSuite: cs,
		Random:      rand.Reader,
		Pattern:     noise.HandshakeNX,
		Initiator:   true,
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
			client.cs1 = cs1
			client.cs2 = cs2
			client.state = kStateEstablished
			client.readBuffer = bytes.NewBuffer(make([]byte, 1024))
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
			payload, complete := handshakeResultCheck(handshakeState.WriteMessage(nil, writeBuf))
			writeBuf = nil
			if payload != nil {
				err = util.WritePacket(conn, payload)
				if err != nil {
					client.handshakeCh <- err
					break
				}
			}
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
			if complete != -1 {
				if !bytes.Equal(handshakeState.PeerStatic(), client.connectInfo.PublicKey) {
					client.handshakeCh <- errors.New("illegal remote key")
					break
				}
			}
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

func (client *SipcClient) Read(p []byte) (n int, err error) {
	if client.state != kStateEstablished {
		return -1, sipc_error.NOT_CONNECTED
	}

	return util.BufferedRead(client.readBuffer, p, func() ([]byte, error) {
		buf, err := util.ReadPacket(client.connection)
		if err != nil {
			return nil, err
		}
		return client.cs1.Decrypt(nil, nil, buf)
	})
}

func (client *SipcClient) Write(p []byte) (n int, err error) {
	if client.state != kStateEstablished {
		return -1, sipc_error.NOT_CONNECTED
	}

	ct, err := client.cs2.Encrypt(nil, nil, p)
	if err != nil {
		return 0, err
	}

	err = util.WritePacket(client.connection, ct)
	if err != nil {
		return 0, err
	}

	return len(p), nil
}

func (client *SipcClient) Close() error {
	if client.state != kStateEstablished {
		return sipc_error.NOT_CONNECTED
	}

	client.state = kStateClosed

	return nil
}
