import com.sun.jna.platform.win32.Kernel32;
import kr.jclab.sipc.OsDetector;

public class TestUtil {
    public static long getPid() {
        if (OsDetector.IS_WINDOWS) {
            return Kernel32.INSTANCE.GetCurrentProcessId();
        } else {
            return LinuxTestUtil.CLibrary.INSTANCE.getpid();
        }
    }
}
