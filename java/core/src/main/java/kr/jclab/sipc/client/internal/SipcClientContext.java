package kr.jclab.sipc.client.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPromise;
import kr.jclab.noise.protocol.DHState;
import kr.jclab.noise.protocol.Noise;
import kr.jclab.sipc.proto.SipcProto;
import lombok.Getter;
import lombok.Setter;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SipcClientContext {
    @Getter
    private final SipcProto.ConnectInfo connectInfo;
    @Getter
    private final ChannelHandler channelHandler;

    @Getter
    private final DHState localPrivateKey;

    @Setter
    public ChannelPromise handshakeFuture;

    @Getter
    private Channel channel;

    public SipcClientContext(SipcProto.ConnectInfo connectInfo, ChannelHandler channelHandler) throws NoSuchAlgorithmException {
        this.connectInfo = connectInfo;
        this.channelHandler = channelHandler;

        this.localPrivateKey = Noise.createDH("25519");
        this.localPrivateKey.generateKeyPair();
    }

    public void setChannel(Channel channel) {
        this.handshakeFuture = channel.newPromise();
    }

    public Future<Void> handshakeFuture() {
        return this.handshakeFuture;
    }

    public void onHandshakeComplete(Channel ch) {
        ch.pipeline().addLast(this.channelHandler);

        handshakeFuture.setSuccess();
    }

    public void onHandshakeFailed(Throwable e) {
        handshakeFuture.setFailure(e);
    }

    public void shutdown() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
    }
}
