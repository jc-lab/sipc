package kr.jclab.sipc.internal.noise;

public interface NoiseHandler {
    default byte[] getInitiatorMessage() {
        return null;
    }

    default void onReadMessage(NoiseNXHandshake handshake, byte[] payload) {}

    default void onHandshakeComplete(NoiseNXHandshake handshake, NoiseSecureChannelSession session) {}

    default void onHandshakeFailed(NoiseNXHandshake handshake, Throwable e) {}
}
