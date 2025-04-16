//go:build !windows && !darwin
// +build !windows,!darwin

package transport

import (
	"context"
	"errors"
	"github.com/google/uuid"
	"github.com/jc-lab/sipc/go/sipc_error"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"net"
	"syscall"
)

type DomainSocketTransport struct{}

var _ Transport = (*DomainSocketTransport)(nil)

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

func (t *DomainSocketTransport) ConnectContext(ctx context.Context, path string) (net.Conn, error) {
	dialer := &net.Dialer{}
	return dialer.DialContext(ctx, "unix", path)
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

	f, err := unixConn.File() // c.fd.dup()
	if err != nil {
		return nil, err
	}
	defer f.Close()

	cred, err := syscall.GetsockoptUcred(int(f.Fd()), syscall.SOL_SOCKET, syscall.SO_PEERCRED)
	if err != nil {
		return nil, err
	}

	output := &PeerCredentials{
		Pid: int(cred.Pid),
	}

	return output, nil
}
