package kr.jclab.sipc.exception;

public class NotYetConnectedException extends SipcException {
    public NotYetConnectedException() {
    }

    public NotYetConnectedException(String message) {
        super(message);
    }

    public NotYetConnectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotYetConnectedException(Throwable cause) {
        super(cause);
    }

    public NotYetConnectedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
