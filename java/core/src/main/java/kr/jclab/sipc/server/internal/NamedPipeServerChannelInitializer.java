package kr.jclab.sipc.server.internal;

import kr.jclab.netty.channel.iocp.NamedPipeChannel;

public class NamedPipeServerChannelInitializer extends ServerChannelInitializer<NamedPipeChannel> {
    public NamedPipeServerChannelInitializer(SipcServerContext serverContext) {
        super(serverContext);
    }

    @Override
    protected void initChannel(NamedPipeChannel ch) throws Exception {
        this.doInitChannel(ch, ch.peerCredentials().pid());
    }
}
