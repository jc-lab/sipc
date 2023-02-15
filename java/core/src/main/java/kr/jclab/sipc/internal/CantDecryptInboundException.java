package kr.jclab.sipc.internal;

public class CantDecryptInboundException extends RuntimeException {
    public CantDecryptInboundException() {
    }

    public CantDecryptInboundException(String message) {
        super(message);
    }

    public CantDecryptInboundException(String message, Throwable cause) {
        super(message, cause);
    }

    public CantDecryptInboundException(Throwable cause) {
        super(cause);
    }

    public CantDecryptInboundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
