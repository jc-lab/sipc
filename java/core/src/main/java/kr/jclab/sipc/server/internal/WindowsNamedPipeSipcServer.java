package kr.jclab.sipc.server.internal;

import com.google.protobuf.ByteString;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.unix.ServerDomainSocketChannel;
import kr.jclab.netty.channel.iocp.IocpEventLoopGroup;
import kr.jclab.netty.channel.iocp.NamedPipeServerChannel;
import kr.jclab.netty.channel.iocp.NamedPipeSocketAddress;
import kr.jclab.noise.protocol.DHState;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.platform.WindowsNativeSupport;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.SipcChild;
import kr.jclab.sipc.server.SipcServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class WindowsNamedPipeSipcServer extends SipcServer {
    private final NamedPipeServerChannel serverChannel;
    private final String transportAddress;

    public WindowsNamedPipeSipcServer(
            EventLoopHolder eventLoopHolder,
            DHState localPrivateKey,
            SocketAddress localAddress,
            int handshakeTimeoutMilliseconds,
            WindowsNativeSupport windowsNativeSupport
    ) throws NoSuchAlgorithmException {
        super(
                new SipcServerContext(
                        eventLoopHolder,
                        localPrivateKey,
                        SipcProto.TransportType.kWindowsNamedPipe,
                        windowsNativeSupport
                )
        );

        this.transportAddress = ((NamedPipeSocketAddress) localAddress).getName();

        if (!eventLoopHolder.isGroupPresent()) {
            eventLoopHolder.initialize(new IocpEventLoopGroup(1), new IocpEventLoopGroup(1));
        }

        if (handshakeTimeoutMilliseconds != 0) {
            serverContext.setHandshakeTimeout(handshakeTimeoutMilliseconds);
        }

        this.serverChannel = (NamedPipeServerChannel) new ServerBootstrap()
                .group(eventLoopHolder.getBoss(), eventLoopHolder.getWorker())
                .channel(NamedPipeServerChannel.class)
                    .childHandler(new NamedPipeServerChannelInitializer(serverContext))
                .bind(localAddress)
                .channel();
    }

    @Override
    protected SipcChild createChild(ChannelHandler channelHandler) {
        String connectionId = UUID.randomUUID().toString();
        byte[] publicKey = this.serverContext.getLocalPublicKey();

        SipcProto.ConnectInfo connectInfo = SipcProto.ConnectInfo.newBuilder()
                .setConnectionId(connectionId)
                .setTransportType(this.serverContext.getTransportType())
                .setTransportAddress(transportAddress)
                .setPublicKey(ByteString.copyFrom(publicKey))
                .build();
        SipcChild child = new SipcChild(this, connectInfo, channelHandler);
        serverContext.getChildMapByConnectionId().put(connectionId, child);
        return child;
    }
}
