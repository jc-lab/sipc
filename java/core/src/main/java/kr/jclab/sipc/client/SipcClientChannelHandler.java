package kr.jclab.sipc.client;

import io.netty.channel.ChannelHandler;

public interface SipcClientChannelHandler extends ChannelHandler {
    void onHandshakeTimeout();
}
