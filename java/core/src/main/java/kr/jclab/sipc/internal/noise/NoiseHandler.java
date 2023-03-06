package kr.jclab.sipc.internal.noise;

import java.util.concurrent.CompletableFuture;

public interface NoiseHandler {
    default byte[] beforeWriteMessage(NoiseHandshakeChannelHandler handshake) {
        return null;
    }

    default CompletableFuture<Boolean> onReadMessage(NoiseHandshakeChannelHandler handshake, byte[] payload) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        completableFuture.complete(true);
        return completableFuture;
    }

    default void onHandshakeComplete(NoiseHandshakeChannelHandler handshake, NoiseSecureChannelSession session) {}

    default void onHandshakeFailed(NoiseHandshakeChannelHandler handshake, Throwable e) {}
}
