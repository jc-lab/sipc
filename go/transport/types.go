package transport

import (
	"context"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"net"
)

type PeerCredentials struct {
	Pid int
}

type Transport interface {
	TransportType() sipc_proto.TransportType
	Connect(path string) (net.Conn, error)
	ConnectContext(ctx context.Context, path string) (net.Conn, error)
	Listen(path string) (net.Listener, error)
	NewDefaultPath() string
	GetPeerCredentials(conn net.Conn) (*PeerCredentials, error)
}
