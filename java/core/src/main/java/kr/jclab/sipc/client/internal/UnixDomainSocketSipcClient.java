package kr.jclab.sipc.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import kr.jclab.sipc.OsDetector;
import kr.jclab.sipc.client.SipcClient;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.proto.SipcProto;

import java.security.NoSuchAlgorithmException;

public class UnixDomainSocketSipcClient extends SipcClient {
    public UnixDomainSocketSipcClient(
            EventLoopHolder eventLoopHolder,
            SipcProto.ConnectInfo connectInfo,
            ChannelHandler handler
    ) throws NoSuchAlgorithmException {
        super(eventLoopHolder, connectInfo, handler);
    }

    @Override
    protected Channel createChannel() {
        Bootstrap bootstrap = new Bootstrap();
        if (OsDetector.IS_BSD) {
            if (!eventLoopHolder.isGroupPresent()) {
                eventLoopHolder.initialize(null, new KQueueEventLoopGroup(1));
            }
            bootstrap = bootstrap.channel(KQueueDomainSocketChannel.class);
        } else {
            if (!eventLoopHolder.isGroupPresent()) {
                eventLoopHolder.initialize(null, new EpollEventLoopGroup(1));
            }
            bootstrap = bootstrap.channel(EpollDomainSocketChannel.class);
        }

        return bootstrap
                .group(eventLoopHolder.getWorker())
                .handler(clientChannelInitializer)
                .connect(new DomainSocketAddress(clientContext.getConnectInfo().getTransportAddress()))
                .channel();
    }
}
