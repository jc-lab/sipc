package kr.jclab.sipc.internal;

public class SipcUtils {
    public static boolean hasCauseOfType(Throwable cause, Class<?> clazz) {
        while (cause != null){
            if (clazz.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
