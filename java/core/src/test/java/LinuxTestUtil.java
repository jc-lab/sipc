import com.sun.jna.Library;
import com.sun.jna.Native;

public class LinuxTestUtil {
    public interface CLibrary extends Library {
        CLibrary INSTANCE = (CLibrary) Native.load("c", CLibrary.class);
        int getpid ();
    }
}
