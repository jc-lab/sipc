package kr.jclab.sipc.client.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import kr.jclab.sipc.internal.PacketCoder;
import kr.jclab.sipc.internal.InactiveHandler;
import kr.jclab.sipc.internal.noise.*;
import kr.jclab.sipc.proto.SipcProto;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@ChannelHandler.Sharable
public class ClientChannelInitializer extends ChannelInitializer<Channel> {
    private final SipcClientContext clientContext;
    private final Consumer<ChannelHandlerContext> inactiveHandler;

    public ClientChannelInitializer(SipcClientContext clientContext, Consumer<ChannelHandlerContext> inactiveHandler) {
        this.clientContext = clientContext;
        this.inactiveHandler = inactiveHandler;
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ch.pipeline().addLast(new InactiveHandler(inactiveHandler));
        AtomicInteger handshakeStep = new AtomicInteger(0);

        NoiseHandshakeChannelHandler handshake = new NoiseHandshakeChannelHandler(NoiseRole.INITIATOR, new NoiseHandler() {
            @Override
            public byte[] beforeWriteMessage(NoiseHandshakeChannelHandler handshake) {
                if (handshakeStep.incrementAndGet() == 1) {
                    return SipcProto.ClientHelloPayload.newBuilder()
                            .setConnectionId(clientContext.getConnectInfo().getConnectionId())
                            .build()
                            .toByteArray();
                }
                return null;
            }

            @Override
            public CompletableFuture<Boolean> onReadMessage(NoiseHandshakeChannelHandler handshake, byte[] payload) {
                CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
                byte[] actualPublicKey = new byte[32];
                handshake.getHandshakeState().getRemotePublicKey().getPublicKey(actualPublicKey, 0);
                if (!Arrays.equals(actualPublicKey, clientContext.getConnectInfo().getPublicKey().toByteArray())) {
                    completableFuture.completeExceptionally(new NoiseHandshakeException("different remote public key"));
                } else {
                    completableFuture.complete(true);
                }
                return completableFuture;
            }

            @Override
            public void onHandshakeComplete(NoiseHandshakeChannelHandler handshake, NoiseSecureChannelSession session) {
                clientContext.onHandshakeComplete(ch);
            }

            @Override
            public void onHandshakeFailed(NoiseHandshakeChannelHandler handshake, Throwable e) {
                clientContext.onHandshakeFailed(e);
            }
        });
        handshake.getHandshakeState().getLocalKeyPair().copyFrom(clientContext.getLocalPrivateKey());
        handshake.getHandshakeState().getRemotePublicKey().setPublicKey(clientContext.getConnectInfo().getPublicKey().toByteArray(), 0);
        handshake.start();

        ch.pipeline().addLast(new PacketCoder.Encoder());
        ch.pipeline().addLast(new PacketCoder.Decoder());
        ch.pipeline().addLast(NoiseHandshakeChannelHandler.HANDLER_NAME, handshake);
    }
}
