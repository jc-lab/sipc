package kr.jclab.sipc.server.internal;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.unix.DomainSocketChannel;

@ChannelHandler.Sharable
public class EpollDomainServerChannelInitializer extends ServerChannelInitializer<EpollDomainSocketChannel> {
    public EpollDomainServerChannelInitializer(SipcServerContext serverContext) {
        super(serverContext);
    }

    @Override
    protected void initChannel(EpollDomainSocketChannel ch) throws Exception {
        this.doInitChannel(ch, ch.peerCredentials());
    }
}
