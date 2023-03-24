package kr.jclab.sipc.server;

import io.netty.channel.ChannelHandler;

import javax.annotation.Nullable;

public interface SipcServerChannelHandler extends ChannelHandler {
    void onHandshakeFailed(SipcChild child, Throwable cause);

    void onRemoved(SipcChild child, @Nullable Throwable cause);
}
