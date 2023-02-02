package kr.jclab.sipc.internal;

import kr.jclab.sipc.OsDetector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PidAccessor {
    public static long getPid(Process p) {
        try {
            Method method = Process.class.getDeclaredMethod("pid");
            return (Long) method.invoke(p);
        } catch (NoSuchMethodException noSuchMethodException) {
            // continue;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        long result = 0;
        try {
            if (p.getClass().getName().equals("java.lang.Win32Process") ||
                    (OsDetector.IS_WINDOWS && p.getClass().getName().equals("java.lang.ProcessImpl"))) {
                WindowsJnaSupport windowsJnaSupport = JnaSupport.getWindowsJnaSupport();
                return windowsJnaSupport.getPidFromProcess(p);
            }
            if (p.getClass().getName().equals("java.lang.UNIXProcess"))
            {
                synchronized (p) {
                    Field f = p.getClass().getDeclaredField("pid");
                    f.setAccessible(true);
                    result = f.getLong(p);
                    f.setAccessible(false);
                }
                return result;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
}
