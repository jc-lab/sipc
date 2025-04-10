package test

import (
	"github.com/jc-lab/sipc/go/client"
	"github.com/jc-lab/sipc/go/server"
	"io"
	"os"
	"sync"
	"testing"
)

func newBenchPipe(t *testing.B) (*server.SipcChild, *client.SipcClient) {
	var wg sync.WaitGroup

	sipcServer, err := server.NewSipcServer(server.SipcServerConfig{})
	if err != nil {
		t.Fatal(err)
	}

	t.Cleanup(func() {
		sipcServer.Shutdown()
	})

	sipcChild, err := sipcServer.PrepareProcess(&server.SipcChildConfig{})
	if err != nil {
		t.Fatal(err)
	}
	sipcChild.AttachProcessWithPid(os.Getpid())

	wg.Add(1)
	go func() {
		defer wg.Done()
		if err := sipcChild.Start(); err != nil {
			t.Fatal(err)
		}
	}()

	sipcClient, err := client.NewSipcClient(sipcChild.GetEncodedConnectInfo())
	if err != nil {
		t.Fatal(err)
	}
	if err = sipcClient.Start(); err != nil {
		t.Fatal(err)
	}

	wg.Wait()

	return sipcChild, sipcClient
}

// Different payload sizes
func BenchmarkThroughput1KB(b *testing.B)   { benchmarkThroughputSize(b, 1024) }
func BenchmarkThroughput4KB(b *testing.B)   { benchmarkThroughputSize(b, 4096) }
func BenchmarkThroughput64KB(b *testing.B)  { benchmarkThroughputSize(b, 64*1024) }
func BenchmarkThroughput256KB(b *testing.B) { benchmarkThroughputSize(b, 256*1024) }

func benchmarkThroughputSize(b *testing.B, payloadSize int) {
	serverConn, clientConn := newBenchPipe(b)
	defer serverConn.Close()
	defer clientConn.Close()

	payload := make([]byte, payloadSize)
	for i := range payload {
		payload[i] = byte(i % 256)
	}

	var wg sync.WaitGroup

	// Start server
	wg.Add(1)
	go func() {
		defer wg.Done()

		buf := make([]byte, payloadSize)
		for {
			n, err := serverConn.Read(buf)
			if err != nil {
				if err != io.EOF {
					b.Errorf("Server read error: %v", err)
				}
				return
			}
			_, err = serverConn.Write(buf[:n]) // Echo back
			if err != nil {
				b.Errorf("Server write error: %v", err)
				return
			}
		}
	}()

	buf := make([]byte, payloadSize)

	b.ResetTimer()
	b.SetBytes(int64(payloadSize * 2)) // Count both directions

	// Run benchmark
	for i := 0; i < b.N; i++ {
		// Write payload
		_, err := clientConn.Write(payload)
		if err != nil {
			b.Fatalf("Write failed: %v", err)
		}

		// Read echo
		_, err = io.ReadFull(clientConn, buf)
		if err != nil {
			b.Fatalf("Read failed: %v", err)
		}
	}

	b.StopTimer()
	clientConn.Close()
	wg.Wait()
}
