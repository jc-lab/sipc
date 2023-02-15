package kr.jclab.sipc.client.internal;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPromise;
import kr.jclab.sipc.proto.SipcProto;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SipcClientContext {
    @Getter
    private final SipcProto.ConnectInfo connectInfo;
    @Getter
    private final ChannelHandler channelHandler;

    @Setter
    public ChannelPromise handshakeFuture;

    @Getter
    private Channel channel;

    public SipcClientContext(SipcProto.ConnectInfo connectInfo, ChannelHandler channelHandler) {
        this.connectInfo = connectInfo;
        this.channelHandler = channelHandler;
    }

    public void setChannel(Channel channel) {
        this.handshakeFuture = channel.newPromise();
    }

    public Future<Void> handshakeFuture() {
        return this.handshakeFuture;
    }

    public void onHandshakeComplete() {
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
