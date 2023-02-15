package util

import (
	"bytes"
	"testing"
)

var input = []byte("AAAA0123456789abcdefBBBB0123456789abcdef")

func TestBufferedReadCase1(t *testing.T) {
	buffer := bytes.NewBuffer(make([]byte, 0))
	buffer.Reset()

	output := make([]byte, 20)
	for i := 0; i < 2; i++ {
		n, _ := BufferedRead(buffer, output, func() ([]byte, error) {
			return input[0:20], nil
		})
		if n != 20 {
			t.Fatal("invalid output length")
		}
		if !bytes.Equal(output, input[0:20]) {
			t.Fatal("invalid output content")
		}
	}
}

func TestBufferedReadCase2(t *testing.T) {
	buffer := bytes.NewBuffer(make([]byte, 0))
	buffer.Reset()

	writePos := 0
	readPos := 0

	output := make([]byte, 4)
	for i := 0; i < 5; i++ {
		n, _ := BufferedRead(buffer, output, func() ([]byte, error) {
			if writePos >= len(input) {
				return []byte{}, nil
			}
			t := input[writePos : writePos+4]
			writePos += 4
			return t, nil
		})
		if n != 4 {
			t.Fatal("invalid output length")
		}
		if !bytes.Equal(output, input[readPos:readPos+4]) {
			t.Fatal("invalid output content")
		}
		readPos += 4
	}
}

func TestBufferedReadCase3(t *testing.T) {
	buffer := bytes.NewBuffer(make([]byte, 16))
	buffer.Reset()

	writePos := 0
	readPos := 0

	output := make([]byte, 4)
	for i := 0; i < 5; i++ {
		n, _ := BufferedRead(buffer, output, func() ([]byte, error) {
			if writePos >= len(input) {
				return []byte{}, nil
			}
			t := input[writePos : writePos+20]
			writePos += 20
			return t, nil
		})
		if n != 4 {
			t.Fatal("invalid output length")
		}
		if !bytes.Equal(output, input[readPos:readPos+4]) {
			t.Fatal("invalid output content")
		}
		readPos += 4
	}
}
