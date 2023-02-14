package promise

import (
	"sync"
	"testing"
	"time"
)

func TestNew(t *testing.T) {
	p := NewPromise[int]()
	if p == nil {
		t.Fail()
	}
}

func TestSignalAndWait(t *testing.T) {
	p := NewPromise[int]()

	p.Complete(123)
	if p.Wait() != 123 {
		t.Fail()
	}
}

func TestWaitAndSignal(t *testing.T) {
	p := NewPromise[int]()

	go func() {
		time.Sleep(time.Millisecond * 100)
		p.Complete(123)
	}()

	if p.Wait() != 123 {
		t.Fail()
	}
}

func TestSignalAndChan(t *testing.T) {
	p := NewPromise[int]()

	p.Complete(123)
	v := <-p.Chan()
	if v != 123 {
		t.Fail()
	}
}

func TestChanAndSignal(t *testing.T) {
	p := NewPromise[int]()

	go func() {
		time.Sleep(time.Millisecond * 100)
		p.Complete(123)
	}()

	v := <-p.Chan()
	if v != 123 {
		t.Fail()
	}
}

func TestChanMixed(t *testing.T) {
	p := NewPromise[int]()

	wg := sync.WaitGroup{}
	wg.Add(1)

	go func() {
		v := <-p.Chan()
		if v != 123 {
			t.Fail()
		}
		wg.Done()
	}()

	go func() {
		time.Sleep(time.Millisecond * 100)
		p.Complete(123)
	}()

	v := <-p.Chan()
	if v != 123 {
		t.Fail()
	}

	wg.Wait()
}
