package kr.jclab.sipc.server;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerDomainSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import io.netty.channel.unix.ServerDomainSocketChannel;
import kr.jclab.sipc.OsDetector;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.internal.*;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class SipcServer {

    final SipcServerContext serverContext;

    public SipcServer(
            SipcServerContext serverContext
    ) throws NoSuchAlgorithmException {
        this.serverContext = serverContext;
    }

    @lombok.Builder(builderClassName = "Builder")
    static SipcServer create(
            EventLoopGroup boss,
            EventLoopGroup worker,
            ScheduledExecutorService scheduledExecutorService,
            SocketAddress localAddress,
            int handshakeTimeoutMilliseconds
    ) throws NoSuchAlgorithmException {
        EventLoopHolder eventLoopHolder = new EventLoopHolder();

        if (worker != null) {
            eventLoopHolder.useExternal(boss, worker);
        }
        if (scheduledExecutorService != null) {
            eventLoopHolder.useExternal(scheduledExecutorService);
        }

        SipcServerContext serverContext;
        SipcServer serverImpl;

        ServerBootstrap bootstrap = new ServerBootstrap();
        if (OsDetector.IS_WINDOWS) {
            if (!eventLoopHolder.isGroupPresent()) {
                eventLoopHolder.initialize(new NioEventLoopGroup(1), new NioEventLoopGroup(1));
            }

            if (localAddress == null) {
                localAddress = new InetSocketAddress("127.0.0.1", 0);
            }

            serverContext = new SipcServerContext(
                    eventLoopHolder,
                    null,
                    SipcProto.TransportType.kWindowsNamedPipe,
                    localAddress.toString()
            );

            Channel channel = bootstrap
                    .group(eventLoopHolder.getBoss(), eventLoopHolder.getWorker())
                    .channel(ServerDomainSocketChannel.class)
//                    .childHandler(new ServerChannelInitializer())
                    .bind(localAddress)
                    .channel();


            serverImpl = new WindowsNamedPipeSipcServer(null, serverContext);
        } else {
            if (localAddress == null) {
                File file = new File("/tmp/", UUID.randomUUID().toString() + ".sock");
                localAddress = new DomainSocketAddress(file);
            }

            serverContext = new SipcServerContext(
                    eventLoopHolder,
                    null,
                    SipcProto.TransportType.kUnixDomainSocket,
                    localAddress.toString()
            );

            Channel channel;

            if (OsDetector.IS_BSD) {
                if (!eventLoopHolder.isGroupPresent()) {
                    eventLoopHolder.initialize(new KQueueEventLoopGroup(1), new KQueueEventLoopGroup(1));
                }

                channel = bootstrap
                        .group(eventLoopHolder.getBoss(), eventLoopHolder.getWorker())
                        .channel(KQueueServerDomainSocketChannel.class)
                        .childHandler(new KqueueDomainServerChannelInitializer(serverContext))
                        .bind(localAddress)
                        .channel();
            } else {
                if (!eventLoopHolder.isGroupPresent()) {
                    eventLoopHolder.initialize(new EpollEventLoopGroup(1), new EpollEventLoopGroup(1));
                }

                channel = bootstrap
                        .group(eventLoopHolder.getBoss(), eventLoopHolder.getWorker())
                        .channel(EpollServerDomainSocketChannel.class)
                        .childHandler(new EpollDomainServerChannelInitializer(serverContext))
                        .bind(localAddress)
                        .channel();
            }

            serverImpl = new UnixDomainSocketSipcServer(channel, serverContext);
        }

        if (handshakeTimeoutMilliseconds != 0) {
            serverContext.setHandshakeTimeout(handshakeTimeoutMilliseconds);
        }
        return serverImpl;
    }

    public void shutdown() {
        serverContext.shutdown();
    }

    public SipcChild createProcess(ProcessBuilder processBuilder, ChannelHandler channelHandler) throws IOException {
        SipcChild child = createChild(channelHandler);
        processBuilder.environment().put("SIPC_V1_CONNECT_INFO", child.getEncodedConnectInfo());
        Process process = processBuilder.start();
        child.attachProcess(process);
        return child;
    }

    public SipcChild prepareProcess(ChannelHandler channelHandler) {
        return createChild(channelHandler);
    }

    private SipcChild createChild(ChannelHandler channelHandler) {
        String connectionId = UUID.randomUUID().toString();
        byte[] publicKey = this.serverContext.getLocalPublicKey();

        SipcProto.ConnectInfo connectInfo = SipcProto.ConnectInfo.newBuilder()
                .setConnectionId(connectionId)
                .setTransportType(this.serverContext.getTransportType())
                .setTransportAddress(this.serverContext.getTransportAddress())
                .setPublicKey(ByteString.copyFrom(publicKey))
                .build();
        SipcChild child = new SipcChild(this, connectInfo, channelHandler);
        serverContext.getChildMapByConnectionId().put(connectionId, child);
        return child;
    }
}
