package kr.jclab.sipc.server.internal;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.ServerDomainSocketChannel;
import kr.jclab.noise.protocol.DHState;
import kr.jclab.sipc.OsDetector;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.SipcChild;
import kr.jclab.sipc.server.SipcServer;

import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class UnixDomainSocketSipcServer extends SipcServer {
    private final ServerDomainSocketChannel domainChannel;
    private final String transportAddress;

    public UnixDomainSocketSipcServer(
            EventLoopHolder eventLoopHolder,
            DHState localPrivateKey,
            SocketAddress localAddress,
            int handshakeTimeoutMilliseconds,
            boolean allowReconnect,
            boolean disablePidCheck
    ) throws NoSuchAlgorithmException {
        super(
                new SipcServerContext(
                        eventLoopHolder,
                        localPrivateKey,
                        SipcProto.TransportType.kUnixDomainSocket,
                        null,
                        disablePidCheck
                )
        );

        Channel channel;

        if (handshakeTimeoutMilliseconds != 0) {
            serverContext.setHandshakeTimeout(handshakeTimeoutMilliseconds);
        }
        serverContext.setAllowReconnect(allowReconnect);

        this.transportAddress = ((DomainSocketAddress) localAddress).path();

        if (OsDetector.IS_BSD) {
            if (!eventLoopHolder.isGroupPresent()) {
                eventLoopHolder.initialize(new KQueueEventLoopGroup(1), new KQueueEventLoopGroup(1));
            }

            channel = new ServerBootstrap()
                    .group(eventLoopHolder.getBoss(), eventLoopHolder.getWorker())
                    .channel(KQueueServerDomainSocketChannel.class)
                    .childHandler(new KqueueDomainServerChannelInitializer(serverContext))
                    .bind(localAddress)
                    .channel();
        } else {
            if (!eventLoopHolder.isGroupPresent()) {
                eventLoopHolder.initialize(new EpollEventLoopGroup(1), new EpollEventLoopGroup(1));
            }

            channel = new ServerBootstrap()
                    .group(eventLoopHolder.getBoss(), eventLoopHolder.getWorker())
                    .channel(EpollServerDomainSocketChannel.class)
                    .childHandler(new EpollDomainServerChannelInitializer(serverContext))
                    .bind(localAddress)
                    .channel();
        }

        this.domainChannel = (ServerDomainSocketChannel) channel;
    }

    @Override
    protected SipcChild createChild(ChannelHandler channelHandler) {
        return this.createChild(channelHandler, transportAddress);
    }
}
