package kr.jclab.sipc.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import static com.google.common.base.Preconditions.checkState;

public class DeferredInt {
    private final Object notifyLock = new Object();
    private int value = 0;
    private final List<Consumer<Integer>> callbacks = new ArrayList<>();

    public int get() {
        return value;
    }

    public void compute(Consumer<Integer> callback) {
        synchronized (notifyLock) {
            if (this.value == 0) {
                callbacks.add(callback);
            } else {
                callback.accept(this.value);
            }
        }
    }

    public void set(int newValue) {
        synchronized (notifyLock) {
            checkState(this.value == 0);
            this.value = newValue;
            Iterator<Consumer<Integer>> iterator = callbacks.iterator();
            while (iterator.hasNext()) {
                iterator.next().accept(newValue);
                iterator.remove();
            }
        }
    }
}
