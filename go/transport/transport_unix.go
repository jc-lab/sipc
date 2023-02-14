//go:build !windows
// +build !windows

package transport

import (
	"errors"
	"github.com/google/uuid"
	"github.com/jc-lab/sipc/go/sipc_error"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"net"
	"syscall"
)

type DomainSocketTransport struct {
	Transport
}

func GetDefaultTransportType() sipc_proto.TransportType {
	return sipc_proto.TransportType_kUnixDomainSocket
}

func NewTransport(transportType sipc_proto.TransportType) (Transport, error) {
	if transportType == sipc_proto.TransportType_kUnixDomainSocket {
		return &DomainSocketTransport{}, nil
	}
	return nil, sipc_error.INVALID_TRANSPORT
}

func (t *DomainSocketTransport) TransportType() sipc_proto.TransportType {
	return sipc_proto.TransportType_kUnixDomainSocket
}

func (t *DomainSocketTransport) Connect(path string) (net.Conn, error) {
	return net.Dial("unix", path)
}

func (t *DomainSocketTransport) Listen(path string) (net.Listener, error) {
	return net.Listen("unix", path)
}

func (t *DomainSocketTransport) NewDefaultPath() string {
	return "/tmp/" + uuid.NewString() + ".sock"
}

func (t *DomainSocketTransport) GetPeerCredentials(conn net.Conn) (*PeerCredentials, error) {
	unixConn, ok := conn.(*net.UnixConn)
	if !ok {
		return nil, errors.New("invalid connection")
	}

	f, err := unixConn.File()
	if err != nil {
		return nil, err
	}

	cred, err := syscall.GetsockoptUcred(int(f.Fd()), syscall.SOL_SOCKET, syscall.SO_PEERCRED)
	if err != nil {
		return nil, err
	}

	output := &PeerCredentials{
		Pid: int(cred.Pid),
	}

	return output, nil
}
