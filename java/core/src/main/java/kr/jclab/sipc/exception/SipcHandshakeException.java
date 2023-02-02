package kr.jclab.sipc.exception;

public class SipcHandshakeException extends SipcException {
    public SipcHandshakeException() {
    }

    public SipcHandshakeException(String message) {
        super(message);
    }

    public SipcHandshakeException(String message, Throwable cause) {
        super(message, cause);
    }

    public SipcHandshakeException(Throwable cause) {
        super(cause);
    }

    public SipcHandshakeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
