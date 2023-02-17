package kr.jclab.sipc.server;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import kr.jclab.sipc.exception.SipcHandshakeTimeoutException;
import kr.jclab.sipc.exception.NotYetConnectedException;
import kr.jclab.sipc.internal.DeferredInt;
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

import static com.google.common.base.Preconditions.checkState;

@RequiredArgsConstructor
public class SipcChild {
    private final SipcServer parent;
    private final SipcProto.ConnectInfo connectInfo;
    @Getter
    private final ChannelHandler channelHandler;

    @Getter
    private DeferredInt pid = new DeferredInt();
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
        if (this.process != null || this.getPid().get() != 0) {
            throw new IllegalStateException("already process attached");
        }
        this.process = process;
        this.pid.set((int) PidAccessor.getPid(process, this.parent.serverContext.getWindowsNativeSupport()));
        start();
    }

    public void attachProcess(int pid) {
        if (this.process != null || this.getPid().get() != 0) {
            throw new IllegalStateException("already process attached");
        }
        this.pid.set(pid);
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
            startHandshakeTimeout();
        }
    }

    private synchronized void onHandshakeTimeout() {
        if (this.childChannelContext != null) {
            return;
        }

        parent.serverContext.removeChild(connectInfo.getConnectionId());

        if (!handshakeFuture.isDone()) {
            handshakeFuture.completeExceptionally(new SipcHandshakeTimeoutException());
        }

        if (this.channelHandler instanceof SipcServerChannelHandler) {
            ((SipcServerChannelHandler) this.channelHandler).onHandshakeTimeout(this);
        }
    }

    public synchronized void internalAttachChannel(SipcChildChannelContext childChannelContext) {
        if (!parent.serverContext.isAllowReconnect()) {
            checkState(this.childChannelContext == null);
        }
        this.childChannelContext = childChannelContext;
        if (handshakeTimeoutFuture != null) {
            handshakeTimeoutFuture.cancel(false);
        }
        if (!handshakeFuture.isDone()) {
            handshakeFuture.complete(null);
        }
    }

    public synchronized void internalDetachChannel(SipcChildChannelContext childChannelContext) {
        if (this.childChannelContext == childChannelContext) {
            if (parent.serverContext.isAllowReconnect()) {
                this.childChannelContext = null;
                startHandshakeTimeout();
            } else {
                parent.serverContext.removeChild(connectInfo.getConnectionId());
            }
        }
    }

    private void startHandshakeTimeout() {
        int handshakeTimeout = parent.serverContext.getHandshakeTimeout();
        if (handshakeTimeout > 0) {
            handshakeTimeoutFuture = parent.serverContext.getEventLoopHolder()
                    .getScheduledExecutorService()
                    .schedule(this::onHandshakeTimeout, handshakeTimeout, TimeUnit.MILLISECONDS);
        }
    }
}
