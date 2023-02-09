package kr.jclab.sipc.server.internal;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;

@ChannelHandler.Sharable
public class KqueueDomainServerChannelInitializer extends ServerChannelInitializer<KQueueDomainSocketChannel> {
    public KqueueDomainServerChannelInitializer(SipcServerContext serverContext) {
        super(serverContext);
    }

    @Override
    protected void initChannel(KQueueDomainSocketChannel ch) throws Exception {
        this.doInitChannel(ch, ch.peerCredentials().pid());
    }
}
