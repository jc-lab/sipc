package util

func Chunk(p []byte, maxSize int, handler func(chunk []byte) error) error {
	for len(p) > 0 {
		chunkSize := len(p)
		if chunkSize > maxSize {
			chunkSize = maxSize
		}
		chunkBuf := p[:chunkSize]
		p = p[chunkSize:]
		if err := handler(chunkBuf); err != nil {
			return err
		}
	}
	return nil
}
