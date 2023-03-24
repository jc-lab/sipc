package kr.jclab.sipc.exception;

public class SipcProcessDeadException extends SipcHandshakeException {
    public SipcProcessDeadException() {
    }

    public SipcProcessDeadException(String message) {
        super(message);
    }

    public SipcProcessDeadException(String message, Throwable cause) {
        super(message, cause);
    }

    public SipcProcessDeadException(Throwable cause) {
        super(cause);
    }

    public SipcProcessDeadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
