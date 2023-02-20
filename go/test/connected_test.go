package test

import (
	"encoding/base64"
	"fmt"
	"github.com/jc-lab/sipc/go/client"
	"github.com/jc-lab/sipc/go/server"
	"github.com/jc-lab/sipc/go/sipc_error"
	"github.com/jc-lab/sipc/go/sipc_proto"
	"github.com/jc-lab/sipc/go/transport"
	"github.com/jc-lab/sipc/go/util"
	"google.golang.org/protobuf/proto"
	"io"
	"log"
	"os"
	"sync"
	"testing"
	"time"
)

func Test1(t *testing.T) {
	sipcServer, err := server.NewSipcServer(server.SipcServerConfig{})
	if err != nil {
		log.Fatal(err)
	}

	sipcChild, err := sipcServer.PrepareProcess(&server.SipcChildConfig{})
	if err != nil {
		log.Fatal(err)
	}

	wg := &sync.WaitGroup{}

	// test for lazy pid set
	wg.Add(1)
	go func() {
		time.Sleep(time.Second)
		sipcChild.AttachProcessWithPid(os.Getpid())
		wg.Done()
	}()

	go ClientApp(t, wg, sipcChild.GetEncodedConnectInfo())
	ServerApp(t, wg, sipcChild)

	wg.Wait()
}

func TestHandshakeTimeoutInServer(t *testing.T) {
	sipcServer, err := server.NewSipcServer(server.SipcServerConfig{
		HandshakeTimeout: time.Millisecond * 100,
	})
	if err != nil {
		log.Fatal(err)
	}

	sipcChild, err := sipcServer.PrepareProcess(&server.SipcChildConfig{})
	if err != nil {
		log.Fatal(err)
	}

	wg := &sync.WaitGroup{}

	// test for lazy pid set
	wg.Add(1)
	go func() {
		time.Sleep(time.Second)
		sipcChild.AttachProcessWithPid(os.Getpid())
		wg.Done()
	}()

	err = sipcChild.Start()
	if err != sipc_error.HANDSHAKE_TIMEOUT {
		t.Fatal(err)
	}
	log.Println(err)

	wg.Wait()
}

func TestHandshakeTimeoutInClient(t *testing.T) {
	sipcServer, err := server.NewSipcServer(server.SipcServerConfig{
		HandshakeTimeout: time.Millisecond * 100,
	})
	if err != nil {
		log.Fatal(err)
	}

	sipcChild, err := sipcServer.PrepareProcess(&server.SipcChildConfig{})
	if err != nil {
		log.Fatal(err)
	}

	sipcServer.Shutdown()

	connectInfo := decodeConnectInfo(sipcChild.GetEncodedConnectInfo())
	tp, err := transport.NewTransport(connectInfo.TransportType)
	if err != nil {
		t.Fatal(err)
	}
	dummyListener, _ := tp.Listen(connectInfo.TransportAddress)
	go func() {
		dummyListener.Accept()
	}()
	defer dummyListener.Close()

	sipcClient, err := client.NewSipcClient(sipcChild.GetEncodedConnectInfo())
	if err != nil {
		t.Fatal(err)
	}
	sipcClient.HandshakeTimeout = time.Millisecond * 100

	err = sipcClient.Start()
	if err != sipc_error.HANDSHAKE_TIMEOUT {
		t.Fatal(err)
	}
	t.Log(err)
}

func TestReconnectFailureInServer(t *testing.T) {
	sipcServer, err := server.NewSipcServer(server.SipcServerConfig{
		HandshakeTimeout: time.Millisecond * 100,
		AllowReconnect:   true,
	})
	if err != nil {
		log.Fatal(err)
	}

	failurePromise := util.NewPromise[error]()
	sipcChild, err := sipcServer.PrepareProcess(&server.SipcChildConfig{
		HandshakeFailureHandler: func(child *server.SipcChild, cause error) {
			log.Println(cause)
			failurePromise.Complete(cause)
		},
	})
	if err != nil {
		log.Fatal(err)
	}

	sipcChild.AttachProcessWithPid(os.Getpid())

	wg := &sync.WaitGroup{}

	// test for lazy pid set
	wg.Add(1)
	go func() {
		defer wg.Done()

		sipcClient, err := client.NewSipcClient(sipcChild.GetEncodedConnectInfo())
		if err != nil {
			log.Fatal(err)
		}

		err = sipcClient.Start()
		if err != nil {
			log.Fatal("CLIENT: handshake error", err)
		}

		sipcClient.Close()
	}()

	err = sipcChild.Start()
	if err != nil {
		t.Fatal(err)
	}

	DummyReader(wg, sipcChild)

	log.Println("First connected")

	res, ok := failurePromise.Wait(time.Second * 5)
	if !ok || *res != sipc_error.HANDSHAKE_TIMEOUT {
		t.Fail()
	}

	wg.Wait()
}

func ServerApp(t *testing.T, wg *sync.WaitGroup, sipcChild *server.SipcChild) {
	err := sipcChild.Start()
	if err != nil {
		t.Fatal("SERVER: handshake error", err)
		return
	}

	t.Log("SERVER: handshake ok")

	wg.Add(1)
	go func() {
		buf := make([]byte, 1024)
		for {
			n, err := sipcChild.Read(buf)
			if err != nil {
				if util.ErrIsClose(err) {
					t.Log("sipcChild.Read ", err)
					break
				}
				t.Fatal("SERVER: read error: ", err)
				break
			}
			log.Println(fmt.Sprintf("SERVER: received: %d: %s", n, string(buf[0:n])))
		}
		wg.Done()
	}()

	for i := 0; i < 5; i++ {
		_, err := sipcChild.Write([]byte(fmt.Sprintf("HELLO WORLD I AM SERVER [%d]", i)))
		if err == sipc_error.NOT_CONNECTED || util.ErrIsClose(err) {
			t.Log("sipcChild.Write ", err)
			break
		}
		if err != nil {
			t.Fatal("SERVER: write error: ", err)
			break
		}
		time.Sleep(time.Millisecond * 100)
	}
}

func ClientApp(t *testing.T, wg *sync.WaitGroup, connectInfo string) {
	sipcClient, err := client.NewSipcClient(connectInfo)
	if err != nil {
		log.Fatal(err)
	}

	err = sipcClient.Start()
	if err != nil {
		log.Fatal("CLIENT: handshake error", err)
	}

	log.Println("CLIENT: handshake ok")

	closed := false

	wg.Add(1)
	go func() {
		buf := make([]byte, 1024)
		for !closed {
			n, err := sipcClient.Read(buf)
			if err != nil {
				if closed && util.ErrIsClose(err) {
					t.Log("sipcClient.Read ", err)
					break
				}
				t.Fatal("CLIENT: read error: ", err)
				break
			}
			log.Println(fmt.Sprintf("CLIENT: received: %d: %s", n, string(buf[0:n])))
		}

		wg.Done()
	}()

	for i := 0; i < 5; i++ {
		_, err := sipcClient.Write([]byte(fmt.Sprintf("HELLO WORLD I AM CLIENT [%d]", i)))
		if err != nil {
			t.Fatal("CLIENT: write error: ", err)
			break
		}
		i++
		time.Sleep(time.Millisecond * 100)
	}
	closed = true
	sipcClient.Close()
}

func DummyReader(wg *sync.WaitGroup, reader io.Reader) {
	wg.Add(1)
	go func() {
		buf := make([]byte, 1024)
		for {
			_, err := reader.Read(buf)
			if err != nil {
				break
			}
		}
		wg.Done()
	}()
}

func decodeConnectInfo(encoded string) *sipc_proto.ConnectInfo {
	connectInfoBinary, err := base64.URLEncoding.DecodeString(encoded)
	if err != nil {
	}
	connectInfo := &sipc_proto.ConnectInfo{}
	err = proto.Unmarshal(connectInfoBinary, connectInfo)
	if err != nil {
		log.Fatal(err)
	}
	return connectInfo
}
