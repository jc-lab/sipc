package util

import (
	"sync"
	"testing"
	"time"
)

type DummyStruct struct {
	value int
}

func TestNew(t *testing.T) {
	p := NewPromise[DummyStruct]()
	if p == nil {
		t.Fail()
	}
}

func TestSignalAndWait(t *testing.T) {
	p := NewPromise[DummyStruct]()

	p.Complete(DummyStruct{value: 123})

	r, ok := p.Wait(time.Second)
	if !ok {
		t.Fail()
	}

	if r.value != 123 {
		t.Fail()
	}
}

func TestWaitAndSignal(t *testing.T) {
	p := NewPromise[DummyStruct]()

	go func() {
		time.Sleep(time.Millisecond * 100)
		p.Complete(DummyStruct{value: 123})
	}()

	r, ok := p.Wait(time.Second)
	if !ok {
		t.Fail()
	}

	if r.value != 123 {
		t.Fail()
	}
}

func TestSignalAndChan(t *testing.T) {
	p := NewPromise[DummyStruct]()

	p.Complete(DummyStruct{value: 123})
	v := <-p.Chan()

	if v.value != 123 {
		t.Fail()
	}
}

func TestChanAndSignal(t *testing.T) {
	p := NewPromise[DummyStruct]()

	go func() {
		time.Sleep(time.Millisecond * 100)
		p.Complete(DummyStruct{value: 123})
	}()

	v := <-p.Chan()
	if v.value != 123 {
		t.Fail()
	}
}

func TestChanMixed(t *testing.T) {
	p := NewPromise[DummyStruct]()

	wg := sync.WaitGroup{}
	wg.Add(1)

	go func() {
		v := <-p.Chan()
		if v.value != 123 {
			t.Fail()
		}
		wg.Done()
	}()

	go func() {
		time.Sleep(time.Millisecond * 100)
		p.Complete(DummyStruct{value: 123})
	}()

	v := <-p.Chan()
	if v.value != 123 {
		t.Fail()
	}

	wg.Wait()
}
