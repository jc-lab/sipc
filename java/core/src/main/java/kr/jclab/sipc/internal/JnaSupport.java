package kr.jclab.sipc.internal;

import java.util.concurrent.atomic.AtomicReference;

public class JnaSupport {
    public static boolean isWindowsJnaSupport() {
        try {
            JnaSupport.class.getClassLoader().loadClass("com.sun.jna.platform.win32.Kernel32");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static AtomicReference<WindowsJnaSupport> WINDOWS_JNA_SUPPORT = new AtomicReference<>();

    public static WindowsJnaSupport getWindowsJnaSupport() {
        return WINDOWS_JNA_SUPPORT.updateAndGet((prev) -> {
           if (prev == null && isWindowsJnaSupport()) {
               return new WindowsJnaSupport();
           }
           return null;
        });
    }
}
