package kr.jclab.sipc.exception;

public class SipcException extends RuntimeException {
    public SipcException() {
        super();
    }

    public SipcException(String message) {
        super(message);
    }

    public SipcException(String message, Throwable cause) {
        super(message, cause);
    }

    public SipcException(Throwable cause) {
        super(cause);
    }

    protected SipcException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
