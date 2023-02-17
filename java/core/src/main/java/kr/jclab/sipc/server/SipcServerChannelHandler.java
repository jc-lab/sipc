package kr.jclab.sipc.server;

import io.netty.channel.ChannelHandler;

public interface SipcServerChannelHandler extends ChannelHandler {
    void onHandshakeTimeout(SipcChild child);
}
