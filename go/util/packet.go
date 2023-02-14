package util

import (
	"encoding/binary"
	"io"
	"net"
)

func ReadPacket(conn net.Conn) ([]byte, error) {
	header := make([]byte, 2)
	if _, err := io.ReadFull(conn, header); err != nil {
		return nil, err
	}
	expectedSize := binary.BigEndian.Uint16(header)
	buf := make([]byte, expectedSize)
	if _, err := io.ReadFull(conn, buf); err != nil {
		return nil, err
	}
	return buf, nil
}

func WritePacket(conn net.Conn, payload []byte) error {
	header := make([]byte, 2)
	binary.BigEndian.PutUint16(header, uint16(len(payload)))
	payload = append(header, payload...)
	_, err := conn.Write(payload)
	return err
}
