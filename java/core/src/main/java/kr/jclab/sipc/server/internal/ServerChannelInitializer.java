package kr.jclab.sipc.server.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import kr.jclab.sipc.internal.PacketCoder;
import kr.jclab.sipc.internal.InactiveHandler;
import kr.jclab.sipc.internal.noise.*;
import kr.jclab.sipc.server.SipcChild;
import lombok.extern.slf4j.Slf4j;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@ChannelHandler.Sharable
public abstract class ServerChannelInitializer<C extends Channel> extends ChannelInitializer<C> {
    private final SipcServerContext serverContext;

    public ServerChannelInitializer(SipcServerContext serverContext) {
        this.serverContext = serverContext;
    }

    protected void doInitChannel(Channel ch, int pid) throws NoSuchAlgorithmException {
        ch.attr(ServerConstants.ATTR_PEER_PID).set(pid);

        final SipcChildChannelContext sipcChildChannelContext = new SipcChildChannelContext(serverContext, ch, pid);
        ch.pipeline().addLast(new InactiveHandler((ctx) -> {
            SipcChild sipcChild = sipcChildChannelContext.getSipcChild();
            if (sipcChild != null) {
                sipcChild.internalDetachChannel(sipcChildChannelContext);
            }
        }));

        NoiseHandshakeChannelHandler handshake = new NoiseHandshakeChannelHandler(NoiseRole.RESPONDER, new NoiseHandler() {
            @Override
            public CompletableFuture<Boolean> onReadMessage(NoiseHandshakeChannelHandler handshake, byte[] payload) {
                if (payload != null && sipcChildChannelContext.getState() == SipcChildChannelContext.HandshakeState.HANDSHAKEING_1) {
                    return sipcChildChannelContext.onClientHello(payload);
                }

                CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
                completableFuture.complete(true);
                return completableFuture;
            }

            @Override
            public void onHandshakeComplete(NoiseHandshakeChannelHandler handshake, NoiseSecureChannelSession session) {
                sipcChildChannelContext.noiseHandshakeComplete(ch);
            }

            @Override
            public void onHandshakeFailed(NoiseHandshakeChannelHandler handshake, Throwable e) {
                sipcChildChannelContext.onHandshakeFailure();
            }
        });

        handshake.getHandshakeState().getLocalKeyPair().copyFrom(serverContext.getLocalPrivateKey());
        handshake.start();

        ch.pipeline().addLast(new PacketCoder.Encoder());
        ch.pipeline().addLast(new PacketCoder.Decoder());
        ch.pipeline().addLast(NoiseHandshakeChannelHandler.HANDLER_NAME, handshake);
    }
}
