package kr.jclab.sipc.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import kr.jclab.netty.channel.iocp.IocpEventLoopGroup;
import kr.jclab.netty.channel.iocp.NamedPipeChannel;
import kr.jclab.netty.channel.iocp.NamedPipeSocketAddress;
import kr.jclab.sipc.OsDetector;
import kr.jclab.sipc.client.SipcClient;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.proto.SipcProto;

public class NamedPipeSipcClient extends SipcClient {
    public NamedPipeSipcClient(
            EventLoopHolder eventLoopHolder,
            SipcProto.ConnectInfo connectInfo,
            ChannelHandler handler
    ) {
        super(eventLoopHolder, connectInfo, handler);
    }

    @Override
    protected Channel createChannel() {
        if (!eventLoopHolder.isGroupPresent()) {
            eventLoopHolder.initialize(null, new IocpEventLoopGroup(1));
        }

        return new Bootstrap()
                .group(eventLoopHolder.getWorker())
                .channel(NamedPipeChannel.class)
                .handler(clientChannelInitializer)
                .connect(new NamedPipeSocketAddress(clientContext.getConnectInfo().getTransportAddress()))
                .channel();
    }
}
