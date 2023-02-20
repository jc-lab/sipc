package server

import (
	"bytes"
	"encoding/base64"
	"errors"
	"github.com/flynn/noise"
	"github.com/google/uuid"
	"github.com/jc-lab/sipc/go/sipc_error"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"github.com/jc-lab/sipc/go/util"
	"google.golang.org/protobuf/proto"
	"io"
	"net"
	"os"
	"os/exec"
	"time"
)

type State int

const (
	kStateConnecting State = 1 + iota
	kStateHandshake
	kStateEstablished
	kStateClosed
)

type HandshakeFailureHandler func(child *SipcChild, cause error)

type SipcChildConfig struct {
	HandshakeFailureHandler HandshakeFailureHandler
}

type SipcChild struct {
	io.ReadWriteCloser

	server *SipcServer

	connectInfo        *sipc_proto.ConnectInfo
	encodedConnectInfo string

	process    *os.Process
	pidPromise *util.Promise[int]

	handshakeFailureHandler HandshakeFailureHandler

	state            State
	handshakePromise *util.Promise[error]

	connection net.Conn
	csServer   *noise.CipherState
	csClient   *noise.CipherState
	readBuffer *bytes.Buffer
}

func (server *SipcServer) createChild(config *SipcChildConfig) (*SipcChild, error) {
	child := &SipcChild{
		connectInfo: &sipc_proto.ConnectInfo{
			TransportType:    server.transport.TransportType(),
			TransportAddress: server.listener.Addr().String(),
			ConnectionId:     uuid.NewString(),
			PublicKey:        server.localStaticKey.Public,
			AllowReconnect:   server.allowReconnect,
		},
		server:           server,
		handshakePromise: util.NewPromise[error](),
		state:            kStateConnecting,
		pidPromise:       util.NewPromise[int](),
		readBuffer:       bytes.NewBuffer(make([]byte, 1024)),
	}

	if config != nil {
		child.handshakeFailureHandler = config.HandshakeFailureHandler
	}

	encoded, err := proto.Marshal(child.connectInfo)
	if err != nil {
		return nil, err
	}
	child.encodedConnectInfo = base64.URLEncoding.EncodeToString(encoded)

	server.addChild(child.connectInfo.ConnectionId, child)

	return child, nil
}

func (server *SipcServer) CreateProcess(cmd *exec.Cmd, config *SipcChildConfig) (*SipcChild, error) {
	child, err := server.createChild(config)
	if err != nil {
		return nil, err
	}

	cmd.Env = append(cmd.Env, "SIPC_V1_CONNECT_INFO="+child.GetEncodedConnectInfo())
	err = cmd.Run()
	if err != nil {
		return nil, err
	}

	err = child.AttachProcess(cmd.Process)

	return child, err
}

func (server *SipcServer) PrepareProcess(config *SipcChildConfig) (*SipcChild, error) {
	child, err := server.createChild(config)
	if err != nil {
		return nil, err
	}

	return child, nil
}

func (child *SipcChild) GetEncodedConnectInfo() string {
	return child.encodedConnectInfo
}

func (child *SipcChild) AttachProcess(process *os.Process) error {
	if child.pidPromise.IsFinished() {
		return errors.New("already process attached")
	}
	child.process = process
	pid := process.Pid
	child.pidPromise.Complete(pid)
	return nil
}

func (child *SipcChild) AttachProcessWithPid(pid int) error {
	if child.pidPromise.IsFinished() {
		return errors.New("already process attached")
	}
	child.pidPromise.Complete(pid)
	return nil
}

func (child *SipcChild) Start() error {
	child.startHandshake()
	v, ok := child.handshakePromise.Wait(child.server.handshakeTimeout)
	if ok {
		return *v
	} else {
		return sipc_error.HANDSHAKE_TIMEOUT
	}
}

func (child *SipcChild) IsClosed() bool {
	return child.state == kStateClosed
}

func (child *SipcChild) Read(p []byte) (int, error) {
	if child.state != kStateEstablished {
		return -1, sipc_error.NOT_CONNECTED
	}

	n, err := util.BufferedRead(child.readBuffer, p, func() ([]byte, error) {
		buf, err := util.ReadPacket(child.connection)
		if err != nil {
			return nil, err
		}
		return child.csClient.Decrypt(nil, nil, buf)
	})
	if err != nil {
		child.onInactive(err)
	}
	return n, err
}

func (child *SipcChild) Write(p []byte) (n int, err error) {
	if child.state != kStateEstablished {
		return -1, sipc_error.NOT_CONNECTED
	}

	ct, err := child.csServer.Encrypt(nil, nil, p)
	if err != nil {
		return 0, err
	}

	err = util.WritePacket(child.connection, ct)
	if err != nil {
		return 0, err
	}

	return len(p), nil
}

func (child *SipcChild) closeObject() {
	child.state = kStateClosed
	child.server.removeChild(child.connectInfo.ConnectionId)
}

func (child *SipcChild) Close() error {
	if child.state != kStateEstablished {
		return sipc_error.NOT_CONNECTED
	}

	child.closeObject()

	return child.connection.Close()
}

func (child *SipcChild) startHandshake() {
	go func() {
		child.handshakePromise.Reset()
		select {
		case _ = <-child.handshakePromise.Chan():
			break
		case _ = <-time.After(child.server.handshakeTimeout):
			child.handshakeFailure(sipc_error.HANDSHAKE_TIMEOUT)
		}
	}()
}

func (child *SipcChild) handshakeSuccess(conn net.Conn, cs1 *noise.CipherState, cs2 *noise.CipherState) bool {
	if child.connection != nil && !child.server.allowReconnect {
		return false
	}

	child.connection = conn
	child.csServer = cs2
	child.csClient = cs1
	child.readBuffer.Reset()
	child.state = kStateEstablished
	child.handshakePromise.Complete(nil)

	return true
}

func (child *SipcChild) handshakeFailure(err error) {
	child.closeObject()
	child.handshakePromise.Complete(err)
	if child.handshakeFailureHandler != nil {
		child.handshakeFailureHandler(child, err)
	}
}

func (child *SipcChild) onInactive(cause error) {
	child.connection = nil
	child.csServer = nil
	child.csClient = nil
	if child.server.allowReconnect {
		child.state = kStateConnecting
		child.startHandshake()
	} else {
		child.closeObject()
	}
}
