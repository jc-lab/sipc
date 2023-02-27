package kr.jclab.sipc.server.internal;

import io.netty.channel.Channel;

public class TcpServerChannelInitializer extends ServerChannelInitializer<Channel> {
    public TcpServerChannelInitializer(SipcServerContext serverContext) {
        super(serverContext);
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        this.doInitChannel(ch, -1);
    }
}
