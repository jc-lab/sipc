package kr.jclab.sipc.internal;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public class Process9Helper {
    public static @Nullable CompletableFuture<Process> onExitIfAvailable(Process process) {
        try {
            Method method = Process.class.getMethod("onExit");
            return (CompletableFuture<Process>) method.invoke(process);
        } catch (Exception e) {}
        return null;
    }
}
