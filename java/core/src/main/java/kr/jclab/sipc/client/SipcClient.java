package kr.jclab.sipc.client;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import kr.jclab.sipc.OsDetector;
import kr.jclab.sipc.client.internal.NamedPipeSipcClient;
import kr.jclab.sipc.client.internal.SipcClientContext;
import kr.jclab.sipc.client.internal.UnixDomainSocketSipcClient;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.proto.SipcProto;

import java.util.Base64;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class SipcClient {
    protected final EventLoopHolder eventLoopHolder;
    protected final SipcClientContext clientContext;

    public SipcClient(
            EventLoopHolder eventLoopHolder,
            SipcProto.ConnectInfo connectInfo,
            ChannelHandler handler
    ) {
        this.eventLoopHolder = eventLoopHolder;
        this.clientContext = new SipcClientContext(connectInfo, handler);

        Channel channel = createChannel();
        this.clientContext.setChannel(channel);
    }

    @lombok.Builder
    public static SipcClient create(
            EventLoopGroup worker,
            ScheduledExecutorService scheduledExecutorService,
            String connectInfoText,
            ChannelHandler handler
    ) throws InvalidProtocolBufferException {
        if (connectInfoText == null) {
            connectInfoText = System.getenv("SIPC_V1_CONNECT_INFO");
        }
        checkNotNull(connectInfoText);

        EventLoopHolder eventLoopHolder = new EventLoopHolder();

        if (worker != null) {
            eventLoopHolder.useExternal(null, worker);
        }
        if (scheduledExecutorService != null) {
            eventLoopHolder.useExternal(scheduledExecutorService);
        }

        SipcProto.ConnectInfo connectInfo = SipcProto.ConnectInfo.newBuilder()
                .mergeFrom(Base64.getUrlDecoder().decode(connectInfoText))
                .build();

        switch (connectInfo.getTransportType()) {
            case kWindowsNamedPipe:
                if (!OsDetector.IS_WINDOWS) {
                    throw new RuntimeException("Invalid transport type this OS. type=" + connectInfo.getTransportType());
                }
                return new NamedPipeSipcClient(eventLoopHolder, connectInfo, handler);
            case kUnixDomainSocket:
                if (OsDetector.IS_WINDOWS) {
                    throw new RuntimeException("Invalid transport type this OS. type=" + connectInfo.getTransportType());
                }
                return new UnixDomainSocketSipcClient(eventLoopHolder, connectInfo, handler);
        }

        throw new RuntimeException("Invalid transport type this OS. type=" + connectInfo.getTransportType());
    }

    public Future<Void> handshakeFuture() {
        return clientContext.handshakeFuture();
    }

    public void shutdown() {
        clientContext.shutdown();
        eventLoopHolder.shutdown();
    }

    protected abstract Channel createChannel();
}
