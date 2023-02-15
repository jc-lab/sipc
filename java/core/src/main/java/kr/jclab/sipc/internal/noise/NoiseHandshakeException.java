package kr.jclab.sipc.internal.noise;

public class NoiseHandshakeException extends RuntimeException {
    public NoiseHandshakeException() {
    }

    public NoiseHandshakeException(String message) {
        super(message);
    }

    public NoiseHandshakeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoiseHandshakeException(Throwable cause) {
        super(cause);
    }

    public NoiseHandshakeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
