package kr.jclab.sipc.internal;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;

import java.lang.reflect.Field;

public class WindowsJnaSupport {
    public int getPidFromProcess(Process p) throws NoSuchFieldException, IllegalAccessException {
        int result;
        synchronized (p) {
            Field f = p.getClass().getDeclaredField("handle");
            f.setAccessible(true);
            long handl = f.getLong(p);
            Kernel32 kernel = Kernel32.INSTANCE;
            WinNT.HANDLE hand = new WinNT.HANDLE();
            hand.setPointer(Pointer.createConstant(handl));
            result = kernel.GetProcessId(hand);
            f.setAccessible(false);
        }
        return result;
    }
}
