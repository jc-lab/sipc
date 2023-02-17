package util

import (
	"io"
	"net"
)

func ErrIsClose(err error) bool {
	return err == io.EOF || err == io.ErrUnexpectedEOF || err == io.ErrClosedPipe || err == net.ErrClosed
}
