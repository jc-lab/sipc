package kr.jclab.sipc.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NotifyMap<K, V> extends ConcurrentHashMap<K, V> {
    private final Object notifyLock = new Object();

    @Override
    public V put(K key, V value) {
        V res = super.put(key, value);
        synchronized (notifyLock) {
            notifyLock.notifyAll();
        }
        return res;
    }

    public V get(K key, int timeout, TimeUnit timeUnit) throws InterruptedException {
        long startedAt = System.nanoTime();
        long limitAt = startedAt + timeUnit.toNanos(timeout);
        V value = null;
        do {
            value = super.get(key);
            long remaining = limitAt - System.nanoTime();
            if (remaining <= 0) {
                break;
            }
            notifyLock.wait();
        } while (value == null);
        return value;
    }
}
