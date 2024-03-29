package kr.jclab.sipc.server;

import com.google.protobuf.ByteString;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.unix.DomainSocketAddress;
import kr.jclab.netty.channel.iocp.IocpEventLoopGroup;
import kr.jclab.netty.channel.iocp.NamedPipeSocketAddress;
import kr.jclab.noise.protocol.DHState;
import kr.jclab.sipc.OsDetector;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.platform.WindowsNativeSupport;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.internal.*;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

public abstract class SipcServer {
    protected final SipcServerContext serverContext;

    public SipcServer(
            SipcServerContext serverContext
    ) throws NoSuchAlgorithmException {
        this.serverContext = serverContext;
    }

    static EventLoopGroup createEventLoopGroup(int nThreads) {
        if (OsDetector.IS_WINDOWS) {
            return new IocpEventLoopGroup(nThreads);
        } else if (OsDetector.IS_BSD) {
            return new KQueueEventLoopGroup(nThreads);
        } else {
            return new EpollEventLoopGroup();
        }
    }

    @lombok.Builder(builderClassName = "Builder")
    static SipcServer create(
            EventLoopGroup boss,
            EventLoopGroup worker,
            ScheduledExecutorService scheduledExecutorService,
            DHState localPrivateKey,
            SocketAddress localAddress,
            int handshakeTimeoutMilliseconds,
            boolean allowReconnect,
            WindowsNativeSupport windowsNativeSupport,
            boolean disablePidCheck
    ) throws NoSuchAlgorithmException {
        EventLoopHolder eventLoopHolder = new EventLoopHolder();

        if (worker != null) {
            eventLoopHolder.useExternal(boss, worker);
        }
        if (scheduledExecutorService != null) {
            eventLoopHolder.useExternal(scheduledExecutorService);
        }

        if (OsDetector.IS_WINDOWS) {
            if (localAddress == null) {
                localAddress = new NamedPipeSocketAddress("\\\\.\\pipe\\" + UUID.randomUUID());
            }

            return new WindowsNamedPipeSipcServer(
                    eventLoopHolder,
                    localPrivateKey,
                    localAddress,
                    handshakeTimeoutMilliseconds,
                    allowReconnect,
                    windowsNativeSupport,
                    disablePidCheck
            );
        } else {
            if (localAddress == null) {
                File file = new File("/tmp/", UUID.randomUUID() + ".sock");
                localAddress = new DomainSocketAddress(file);
            }

            return new UnixDomainSocketSipcServer(
                    eventLoopHolder,
                    localPrivateKey,
                    localAddress,
                    handshakeTimeoutMilliseconds,
                    allowReconnect,
                    disablePidCheck
            );
        }
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

    protected abstract SipcChild createChild(ChannelHandler channelHandler);

    protected SipcChild createChild(ChannelHandler channelHandler, String transportAddress) {
        String connectionId = UUID.randomUUID().toString();
        byte[] publicKey = this.serverContext.getLocalPublicKey();

        SipcProto.ConnectInfo connectInfo = SipcProto.ConnectInfo.newBuilder()
                .setConnectionId(connectionId)
                .setTransportType(this.serverContext.getTransportType())
                .setTransportAddress(transportAddress)
                .setPublicKey(ByteString.copyFrom(publicKey))
                .setAllowReconnect(this.serverContext.isAllowReconnect())
                .build();
        SipcChild child = new SipcChild(this, connectInfo, channelHandler);
        serverContext.addChild(connectionId, child);
        return child;
    }
}
