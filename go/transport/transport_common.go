package transport

import (
	"context"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"net"
)

type TcpTransport struct {
	Transport
}

func NewTcpTransport() *TcpTransport {
	return &TcpTransport{}
}

func (t *TcpTransport) TransportType() sipc_proto.TransportType {
	return sipc_proto.TransportType_kTcp
}

func (t *TcpTransport) Connect(path string) (net.Conn, error) {
	return net.Dial("tcp", path)
}

func (t *TcpTransport) ConnectContext(ctx context.Context, path string) (net.Conn, error) {
	dialer := &net.Dialer{}
	return dialer.DialContext(ctx, "tcp", path)
}

func (t *TcpTransport) Listen(path string) (net.Listener, error) {
	return net.Listen("tcp", path)
}

func (t *TcpTransport) NewDefaultPath() string {
	return "127.0.0.1:0"
}

func (t *TcpTransport) GetPeerCredentials(conn net.Conn) (*PeerCredentials, error) {
	return &PeerCredentials{
		Pid: 0,
	}, nil
}
