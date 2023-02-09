package kr.jclab.sipc.internal;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import kr.jclab.sipc.platform.WindowsNativeSupport;

import java.lang.reflect.Field;

public class WindowsJnaSupport implements WindowsNativeSupport {
    @Override
    public int getPidFromProcess(Process p) throws Exception {
        int result;
        synchronized (p) {
            Field f = p.getClass().getDeclaredField("handle");
            f.setAccessible(true);
            long handleLong = f.getLong(p);
            Kernel32 kernel = Kernel32.INSTANCE;
            WinNT.HANDLE hand = new WinNT.HANDLE();
            hand.setPointer(Pointer.createConstant(handleLong));
            result = kernel.GetProcessId(hand);
            f.setAccessible(false);
        }
        return result;
    }
}
