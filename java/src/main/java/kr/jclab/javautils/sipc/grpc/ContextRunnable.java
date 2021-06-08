package kr.jclab.javautils.sipc.grpc;

import io.grpc.Context;

public abstract class ContextRunnable implements Runnable {
    private final Context context;

    protected ContextRunnable(Context context) {
        this.context = context;
    }

    @Override
    public final void run() {
        Context previous = context.attach();
        try {
            runInContext();
        } finally {
            context.detach(previous);
        }
    }

    public abstract void runInContext();
}

