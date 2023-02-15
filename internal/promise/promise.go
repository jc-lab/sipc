package promise

import (
	"sync"
	"time"
)

type Future[T any] interface {
	Wait(timeout time.Duration) (*T, bool)
	Chan() chan *T
}

type Promise[T any] struct {
	mutex     *sync.Mutex
	cond      *sync.Cond
	consumers []chan *T

	finished bool
	value    *T
}

func NewPromise[T any]() *Promise[T] {
	m := &sync.Mutex{}
	return &Promise[T]{
		mutex: m,
		cond:  sync.NewCond(m),
	}
}

func (p *Promise[T]) Complete(value *T) {
	p.mutex.Lock()
	defer p.mutex.Unlock()
	p.value = value
	p.finished = true
	p.cond.Broadcast()
	for _, c := range p.consumers {
		c <- value
		close(c)
	}
}

func (p *Promise[T]) Wait(timeout time.Duration) (*T, bool) {
	select {
	case res := <-p.Chan():
		return res, true
	case _ = <-time.After(timeout):
		return nil, false
	}
}

func (p *Promise[T]) Chan() chan *T {
	p.mutex.Lock()
	defer p.mutex.Unlock()
	c := make(chan *T, 1)
	if p.finished {
		c <- p.value
		close(c)
	} else {
		p.consumers = append(p.consumers, c)
	}
	return c
}

func (p *Promise[T]) Future() Future[T] {
	return p
}