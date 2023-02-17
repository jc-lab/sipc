package util

import (
	"io"
	"net"
	"strings"
)

func ErrIsClose(err error) bool {
	if err == nil {
		return false
	}
	return err == io.EOF ||
		err == io.ErrUnexpectedEOF ||
		err == io.ErrClosedPipe ||
		err == net.ErrClosed ||
		strings.Contains(err.Error(), "closed") ||
		strings.Contains(err.Error(), "connection reset")
}
