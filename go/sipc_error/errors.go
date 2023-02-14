package sipc_error

import (
	"errors"
)

var HANDSHAKE_TIMEOUT = errors.New("handshake timeout")
var NOT_CONNECTED = errors.New("not connected")
var INVALID_TRANSPORT = errors.New("invalid transport")
