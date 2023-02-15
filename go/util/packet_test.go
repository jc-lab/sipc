package util

import (
	"bytes"
	"io"
	"net"
	"testing"
)

var sendPayload = []byte{0x00, 0x04, 0x01, 0x02, 0x03, 0x04}

func TestReadPacket(t *testing.T) {
	for i := 0; i < 4; i++ {
		a, b := net.Pipe()

		c := sendPayload[0:i]
		d := sendPayload[i:]

		go func() {
			if len(c) > 0 {
				_, _ = a.Write(c)
			}
			_, _ = a.Write(d)
		}()

		packet, err := ReadPacket(b)
		if err != nil {
			t.Fail()
		}

		if !bytes.Equal(packet, []byte{0x01, 0x02, 0x03, 0x04}) {
			t.Fail()
		}
	}
}

func TestWritePacket(t *testing.T) {
	a, b := net.Pipe()
	go func() {
		err := WritePacket(a, []byte{0x01, 0x02, 0x03, 0x04})
		if err != nil {
			t.Fail()
		}
	}()
	readBuffer := make([]byte, 6)
	io.ReadFull(b, readBuffer)

	if !bytes.Equal(readBuffer, sendPayload) {
		t.Fail()
	}
}
