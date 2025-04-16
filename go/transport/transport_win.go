//go:build windows
// +build windows

package transport

import (
	"context"
	"errors"
	"github.com/Microsoft/go-winio"
	"github.com/google/uuid"
	"github.com/jc-lab/sipc/go/sipc_error"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"net"
	"syscall"
	"time"
	"unsafe"
)

var (
	modkernel32                 = syscall.NewLazyDLL("kernel32.dll")
	getNamedPipeClientProcessId = modkernel32.NewProc("GetNamedPipeClientProcessId")
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
	ctx, cancel := context.WithTimeout(context.Background(), time.Second*10)
	defer cancel()
	return t.ConnectContext(ctx, path)
}

func (t *NamedPipeTransport) ConnectContext(ctx context.Context, path string) (net.Conn, error) {
	if ctx.Err() == nil {
		c, err := winio.DialPipeContext(ctx, path)
		if err == nil {
			return c, nil
		} else if !errors.Is(err, winio.ErrTimeout) {
			return c, err
		}
	}
	return nil, ctx.Err()
}

func (t *NamedPipeTransport) Listen(path string) (net.Listener, error) {
	return winio.ListenPipe(path, nil)
}

func (t *NamedPipeTransport) NewDefaultPath() string {
	return "\\\\.\\pipe\\" + uuid.NewString()
}

func (t *NamedPipeTransport) GetPeerCredentials(conn net.Conn) (*PeerCredentials, error) {
	if winFile, ok := conn.(interface{ Fd() uintptr }); ok {
		fd := winFile.Fd()
		var pid uint32 = 0
		err := windowsGetNamedPipeClientProcessId(fd, &pid)
		if err != nil {
			return nil, err
		}

		return &PeerCredentials{
			Pid: int(pid),
		}, nil
	}
	return nil, errors.New("cannot find Fd")
}

func windowsGetNamedPipeClientProcessId(handle uintptr, clientProcessId *uint32) error {
	r1, _, err := getNamedPipeClientProcessId.Call(handle, uintptr(unsafe.Pointer(clientProcessId)))
	if r1 == 1 {
		return nil
	}
	return err
}
