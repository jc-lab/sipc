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

type SipcChild struct {
	io.ReadWriteCloser

	server *SipcServer

	connectInfo        *sipc_proto.ConnectInfo
	encodedConnectInfo string

	process *os.Process
	pid     int

	state       State
	handshakeCh chan error

	connection net.Conn
	cs1        *noise.CipherState
	cs2        *noise.CipherState
	readBuffer *bytes.Buffer
}

func (server *SipcServer) createChild() (*SipcChild, error) {
	child := &SipcChild{
		connectInfo: &sipc_proto.ConnectInfo{
			TransportType:    server.transport.TransportType(),
			TransportAddress: server.listener.Addr().String(),
			ConnectionId:     uuid.NewString(),
			PublicKey:        server.localStaticKey.Public,
		},
		server:      server,
		handshakeCh: make(chan error, 1),
		state:       kStateConnecting,
	}
	encoded, err := proto.Marshal(child.connectInfo)
	if err != nil {
		return nil, err
	}
	child.encodedConnectInfo = base64.StdEncoding.EncodeToString(encoded)

	server.childMap[child.connectInfo.ConnectionId] = child

	return child, nil
}

func (server *SipcServer) CreateProcess(cmd *exec.Cmd) (*SipcChild, error) {
	child, err := server.createChild()
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

func (server *SipcServer) PrepareProcess() (*SipcChild, error) {
	child, err := server.createChild()
	if err != nil {
		return nil, err
	}

	return child, nil
}

func (child *SipcChild) GetEncodedConnectInfo() string {
	return child.encodedConnectInfo
}

func (child *SipcChild) AttachProcess(process *os.Process) error {
	if child.pid != 0 {
		return errors.New("already process attached")
	}
	child.process = process
	child.pid = process.Pid
	return nil
}

func (child *SipcChild) AttachProcessWithPid(pid int) error {
	if child.pid != 0 {
		return errors.New("already process attached")
	}
	child.pid = pid
	return nil
}

func (child *SipcChild) Start() error {
	select {
	case err := <-child.handshakeCh:
		return err
	case _ = <-time.After(child.server.handshakeTimeout):
		return sipc_error.HANDSHAKE_TIMEOUT
	}
}

func (child *SipcChild) Read(p []byte) (n int, err error) {
	if child.state != kStateEstablished {
		return -1, sipc_error.NOT_CONNECTED
	}

	return util.BufferedRead(child.readBuffer, p, func() ([]byte, error) {
		buf, err := util.ReadPacket(child.connection)
		if err != nil {
			return nil, err
		}
		return child.cs2.Decrypt(nil, nil, buf)
	})
}

func (child *SipcChild) Write(p []byte) (n int, err error) {
	if child.state != kStateEstablished {
		return -1, sipc_error.NOT_CONNECTED
	}

	ct, err := child.cs1.Encrypt(nil, nil, p)
	if err != nil {
		return 0, err
	}

	err = util.WritePacket(child.connection, ct)
	if err != nil {
		return 0, err
	}

	return len(p), nil
}

func (child *SipcChild) Close() error {
	if child.state != kStateEstablished {
		return sipc_error.NOT_CONNECTED
	}

	child.state = kStateClosed

	return nil
}

func (child *SipcChild) handshakeSuccess(conn net.Conn, cs1 *noise.CipherState, cs2 *noise.CipherState) {
	child.connection = conn
	child.cs1 = cs1
	child.cs2 = cs2
	child.readBuffer = bytes.NewBuffer(make([]byte, 1024))
	child.readBuffer.Reset()
	child.state = kStateEstablished
	child.handshakeCh <- nil
}

func (child *SipcChild) handshakeFailure(err error) {
	child.handshakeCh <- err
}
