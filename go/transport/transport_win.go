//go:build windows
// +build windows

package transport

import (
	"github.com/Microsoft/go-winio"
	"github.com/google/uuid"
	"github.com/jc-lab/sipc/go/sipc_error"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"net"
)

type NamedPipeTransport struct {
	Transport
}

func GetDefaultTransportType() sipc_proto.TransportType {
	return sipc_proto.TransportType_kWindowsNamedPipe
}

func NewTransport(transportType sipc_proto.TransportType) (Transport, error) {
	if transportType == sipc_proto.TransportType_kWindowsNamedPipe {
		return &NamedPipeTransport{}, nil
	}
	return nil, sipc_error.INVALID_TRANSPORT
}

func (t *NamedPipeTransport) TransportType() sipc_proto.TransportType {
	return sipc_proto.TransportType_kWindowsNamedPipe
}

func (t *NamedPipeTransport) Connect(path string) (net.Conn, error) {
	return winio.DialPipe(path, nil)
}

func (t *NamedPipeTransport) Listen(path string) (net.Listener, error) {
	return winio.ListenPipe(path, nil)
}

func (t *NamedPipeTransport) NewDefaultPath() string {
	return "\\\\.\\pipe\\" + uuid.NewString()
}
