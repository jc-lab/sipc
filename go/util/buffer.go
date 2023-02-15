package util

import (
	"bytes"
)

type ReadFunc func() ([]byte, error)

func BufferedRead(buffer *bytes.Buffer, output []byte, reader ReadFunc) (int, error) {
	totalLength := 0
	if buffer.Len() > 0 {
		n, err := buffer.Read(output)
		if err != nil {
			return 0, err
		}
		output = output[n:]
		totalLength += n
	}

	temp, err := reader()
	if err != nil {
		return 0, err
	}
	avail := cap(output)
	if len(temp) < avail {
		avail = len(temp)
	}
	copy(output, temp[:avail])
	totalLength += avail
	temp = temp[avail:]

	if len(temp) > 0 {
		buffer.Write(temp)
	}

	return totalLength, nil
}
