package test

import (
	"fmt"
	"github.com/jc-lab/sipc/go/client"
	"github.com/jc-lab/sipc/go/server"
	"github.com/jc-lab/sipc/go/sipc_error"
	"github.com/jc-lab/sipc/go/util"
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

	sipcChild, err := sipcServer.PrepareProcess()
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
