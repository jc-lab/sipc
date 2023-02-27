package kr.jclab.sipc.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import kr.jclab.noise.protocol.DHState;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.internal.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

public class SipcTcpServer extends SipcServer {
    private final NioServerSocketChannel serverChannel;
    private final CompletableFuture<String> localAddressPromise = new CompletableFuture<>();

    public SipcTcpServer(
            EventLoopHolder eventLoopHolder,
            DHState localPrivateKey,
            SocketAddress localAddress,
            int handshakeTimeoutMilliseconds,
            boolean allowReconnect
    ) throws NoSuchAlgorithmException, IOException {
        super(
                new SipcServerContext(
                        eventLoopHolder,
                        localPrivateKey,
                        SipcProto.TransportType.kTcp,
                        null
                )
        );

        if (!eventLoopHolder.isGroupPresent()) {
            eventLoopHolder.initialize(new NioEventLoopGroup(1), new NioEventLoopGroup(1));
        }

        if (handshakeTimeoutMilliseconds != 0) {
            serverContext.setHandshakeTimeout(handshakeTimeoutMilliseconds);
        }
        serverContext.setAllowReconnect(allowReconnect);

        this.serverChannel = (NioServerSocketChannel) new ServerBootstrap()
                .group(eventLoopHolder.getBoss(), eventLoopHolder.getWorker())
                .channel(NioServerSocketChannel.class)
                .childHandler(new TcpServerChannelInitializer(serverContext))
                .bind(localAddress)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            InetSocketAddress address = (InetSocketAddress) future.channel().localAddress();
                            localAddressPromise.complete(address.getHostName() + ":" + address.getPort());
                        } else {
                            try {
                                future.get();
                            } catch (Exception e) {
                                localAddressPromise.completeExceptionally(e);
                            }
                        }
                    }
                })
                .channel();
    }

    @lombok.Builder(builderClassName = "TcpBuilder", builderMethodName = "tcpBuilder")
    static SipcServer createTcp(
            EventLoopGroup boss,
            EventLoopGroup worker,
            ScheduledExecutorService scheduledExecutorService,
            DHState localPrivateKey,
            SocketAddress localAddress,
            int handshakeTimeoutMilliseconds,
            boolean allowReconnect
    ) throws NoSuchAlgorithmException, IOException {
        EventLoopHolder eventLoopHolder = new EventLoopHolder();

        if (worker != null) {
            eventLoopHolder.useExternal(boss, worker);
        }
        if (scheduledExecutorService != null) {
            eventLoopHolder.useExternal(scheduledExecutorService);
        }

        localAddress = new InetSocketAddress("127.0.0.1", 0);
        return new SipcTcpServer(
                eventLoopHolder,
                localPrivateKey,
                localAddress,
                handshakeTimeoutMilliseconds,
                allowReconnect
        );
    }

    @Override
    protected SipcChild createChild(ChannelHandler channelHandler) {
        try {
            return this.createChild(channelHandler, localAddressPromise.get());
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
