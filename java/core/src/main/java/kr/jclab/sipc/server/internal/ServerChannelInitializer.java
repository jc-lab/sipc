package kr.jclab.sipc.server.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import kr.jclab.sipc.internal.PacketCoder;
import kr.jclab.sipc.internal.noise.*;
import kr.jclab.sipc.proto.SipcProto;
import lombok.extern.slf4j.Slf4j;

import java.security.NoSuchAlgorithmException;

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

        NoiseNXHandshake handshake = new NoiseNXHandshake(NoiseRole.RESPONDER, new NoiseHandler() {
            @Override
            public void onReadMessage(NoiseNXHandshake handshake, byte[] payload) {
                if (sipcChildChannelContext.getState() == SipcChildChannelContext.HandshakeState.HANDSHAKEING_1) {
                    try {
                        SipcProto.ClientHelloPayload clientHelloPayload = SipcProto.ClientHelloPayload.newBuilder()
                                .mergeFrom(payload)
                                .build();
                        sipcChildChannelContext.onClientHello(clientHelloPayload);
                    } catch (Exception e) {
                        sipcChildChannelContext.onHandshakeFailure();
                        throw new NoiseHandshakeException(e);
                    }
                }
            }

            @Override
            public void onHandshakeComplete(NoiseNXHandshake handshake, NoiseSecureChannelSession session) {
                sipcChildChannelContext.noiseHandshakeComplete(ch);
            }
        });

        handshake.getHandshakeState().getLocalKeyPair().copyFrom(serverContext.getLocalPrivateKey());
        handshake.start();

        ch.pipeline().addLast(new PacketCoder.Encoder());
        ch.pipeline().addLast(new PacketCoder.Decoder());
        ch.pipeline().addLast(NoiseNXHandshake.HANDLER_NAME, handshake);
    }
}
