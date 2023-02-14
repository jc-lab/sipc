package main

import (
	"fmt"
	"github.com/jc-lab/sipc/go/client"
	"github.com/jc-lab/sipc/go/server"
	"log"
	"os"
	"time"
)

func main() {
	sipcServer, err := server.NewSipcServer(server.SipcServerConfig{})
	if err != nil {
		log.Fatal(err)
	}

	sipcChild, err := sipcServer.PrepareProcess()
	if err != nil {
		log.Fatal(err)
	}

	sipcChild.AttachProcessWithPid(os.Getpid())
	go ClientApp(sipcChild.GetEncodedConnectInfo())
	ServerApp(sipcChild)
}

func ServerApp(sipcChild *server.SipcChild) {
	err := sipcChild.Start()
	if err != nil {
		log.Fatal("SERVER: handshake error", err)
	}

	log.Println("SERVER: handshake ok")

	go func() {
		buf := make([]byte, 1024)
		for {
			n, err := sipcChild.Read(buf)
			if err != nil {
				log.Println("SERVER: read error: ", err)
				break
			}
			log.Println(fmt.Sprintf("SERVER: received: %d: %s", n, string(buf[0:n])))
		}
	}()

	var i int = 0
	for {
		_, err := sipcChild.Write([]byte(fmt.Sprintf("HELLO WORLD I AM SERVER [%d]", i)))
		if err != nil {
			log.Println("SERVER: write error: ", err)
			break
		}
		i++
		time.Sleep(time.Second)
	}
}

func ClientApp(connectInfo string) {
	sipcClient, err := client.NewSipcClient(connectInfo)
	if err != nil {
		log.Fatal(err)
	}

	err = sipcClient.Start()
	if err != nil {
		log.Fatal("CLIENT: handshake error", err)
	}

	log.Println("CLIENT: handshake ok")

	go func() {
		buf := make([]byte, 1024)
		for {
			n, err := sipcClient.Read(buf)
			if err != nil {
				log.Println("CLIENT: read error: ", err)
				break
			}
			log.Println(fmt.Sprintf("CLIENT: received: %d: %s", n, string(buf[0:n])))
		}
	}()

	var i int = 0
	for {
		_, err := sipcClient.Write([]byte(fmt.Sprintf("HELLO WORLD I AM CLIENT [%d]", i)))
		if err != nil {
			log.Println("CLIENT: write error: ", err)
			break
		}
		i++
		time.Sleep(time.Second)
	}
}
