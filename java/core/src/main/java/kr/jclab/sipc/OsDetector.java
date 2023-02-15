package kr.jclab.sipc;

public class OsDetector {
    public static final boolean IS_WINDOWS;
    public static final boolean IS_BSD;

    static {
        String osName = System.getProperty("os.name", "generic").toLowerCase();
        IS_WINDOWS = osName.startsWith("win");
        IS_BSD = osName.contains("mac");
    }
}
