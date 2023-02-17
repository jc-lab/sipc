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

        NoiseNXHandshake handshake = new NoiseNXHandshake(NoiseRole.INITIATOR, new NoiseHandler() {
            @Override
            public byte[] getInitiatorMessage() {
                return SipcProto.ClientHelloPayload.newBuilder()
                        .setConnectionId(clientContext.getConnectInfo().getConnectionId())
                        .build()
                        .toByteArray();
            }

            @Override
            public void onReadMessage(NoiseNXHandshake handshake, byte[] payload) {
                byte[] actualPublicKey = new byte[32];
                handshake.getHandshakeState().getRemotePublicKey().getPublicKey(actualPublicKey, 0);
                if (!Arrays.equals(actualPublicKey, clientContext.getConnectInfo().getPublicKey().toByteArray())) {
                    throw new NoiseHandshakeException("different remote public key");
                }
            }

            @Override
            public void onHandshakeComplete(NoiseNXHandshake handshake, NoiseSecureChannelSession session) {
                clientContext.onHandshakeComplete();
            }

            @Override
            public void onHandshakeFailed(NoiseNXHandshake handshake, Throwable e) {
                clientContext.onHandshakeFailed(e);
            }
        });
        handshake.start();

        ch.pipeline().addLast(new PacketCoder.Encoder());
        ch.pipeline().addLast(new PacketCoder.Decoder());
        ch.pipeline().addLast(NoiseNXHandshake.HANDLER_NAME, handshake);
        ch.pipeline().addLast(clientContext.getChannelHandler());
    }
}
