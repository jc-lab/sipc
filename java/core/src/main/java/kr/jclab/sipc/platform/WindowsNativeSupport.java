package kr.jclab.sipc.platform;

public interface WindowsNativeSupport {
    int getPidFromProcess(Process p) throws Exception;
}
