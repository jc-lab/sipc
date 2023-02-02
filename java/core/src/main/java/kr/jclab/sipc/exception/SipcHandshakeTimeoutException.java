package kr.jclab.sipc.exception;

public class SipcHandshakeTimeoutException extends SipcHandshakeException {
    public SipcHandshakeTimeoutException() {
    }

    public SipcHandshakeTimeoutException(String message) {
        super(message);
    }

    public SipcHandshakeTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public SipcHandshakeTimeoutException(Throwable cause) {
        super(cause);
    }

    public SipcHandshakeTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
