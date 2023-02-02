package kr.jclab.sipc.server;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import kr.jclab.sipc.exception.SipcHandshakeTimeoutException;
import kr.jclab.sipc.exception.NotYetConnectedException;
import kr.jclab.sipc.internal.PidAccessor;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.internal.SipcChildChannelContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class SipcChild {
    private final SipcServer parent;
    private final SipcProto.ConnectInfo connectInfo;
    @Getter
    private final ChannelHandler channelHandler;

    @Getter
    private long pid = 0;
    @Getter
    private Process process = null;

    private CompletableFuture<Void> handshakeFuture = new CompletableFuture<>();
    private ScheduledFuture<?> handshakeTimeoutFuture = null;
    private SipcChildChannelContext childChannelContext = null;

    public String getEncodedConnectInfo() {
        return Base64.getUrlEncoder().encodeToString(connectInfo.toByteArray());
    }

    public void attachProcess(Process process) {
        Preconditions.checkNotNull(process);
        if (this.process != null || this.pid != 0) {
            throw new IllegalStateException("already process attached");
        }
        this.process = process;
        this.pid = PidAccessor.getPid(process);
        start();
    }

    public void attachProcess(long pid) {
        if (this.process != null || this.pid != 0) {
            throw new IllegalStateException("already process attached");
        }
        this.pid = pid;
        start();
    }

    public Future<Void> handshakeFuture() {
        return this.handshakeFuture;
    }

    public void writeAndFlush(ByteBuf msg) {
        if (childChannelContext == null) {
            throw new NotYetConnectedException();
        }
        childChannelContext.getChannel().pipeline().writeAndFlush(msg);
    }

    private synchronized void start() {
        if (!handshakeFuture.isDone()) {
            int handshakeTimeout = parent.serverContext.getHandshakeTimeout();
            if (handshakeTimeout > 0) {
                handshakeTimeoutFuture = parent.serverContext.getEventLoopHolder()
                        .getScheduledExecutorService()
                        .schedule(() -> {
                            if (!handshakeFuture.isDone()) {
                                onHandshakeTimeout();
                            }
                        }, handshakeTimeout, TimeUnit.MILLISECONDS);
            }
        }
    }

    private synchronized void onHandshakeTimeout() {
        handshakeFuture.completeExceptionally(new SipcHandshakeTimeoutException());
    }

    public synchronized void internalAttachChannel(SipcChildChannelContext childChannelContext) {
        this.childChannelContext = childChannelContext;
        if (handshakeTimeoutFuture != null) {
            handshakeTimeoutFuture.cancel(false);
        }
        handshakeFuture.complete(null);
    }
}
