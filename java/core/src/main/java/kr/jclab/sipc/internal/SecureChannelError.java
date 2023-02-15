package kr.jclab.sipc.internal;

public class SecureChannelError extends RuntimeException {
    public SecureChannelError() {
    }

    public SecureChannelError(String message) {
        super(message);
    }

    public SecureChannelError(String message, Throwable cause) {
        super(message, cause);
    }

    public SecureChannelError(Throwable cause) {
        super(cause);
    }

    public SecureChannelError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
