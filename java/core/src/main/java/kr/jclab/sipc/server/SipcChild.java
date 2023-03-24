package kr.jclab.sipc.server;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import kr.jclab.sipc.exception.SipcHandshakeTimeoutException;
import kr.jclab.sipc.exception.NotYetConnectedException;
import kr.jclab.sipc.exception.SipcProcessDeadException;
import kr.jclab.sipc.internal.DeferredInt;
import kr.jclab.sipc.internal.PidAccessor;
import kr.jclab.sipc.internal.Process9Helper;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.internal.SipcChildChannelContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
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

        CompletableFuture<Process> onExit = Process9Helper.onExitIfAvailable(process);
        if (onExit != null) {
            onExit.whenComplete((proc, ex) -> {
                if (ex != null) {
                    onProcessDead(ex);
                } else {
                    onProcessDead();
                }
            });
        }
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

    public ChannelFuture writeAndFlush(ByteBuf msg) {
        if (childChannelContext == null) {
            throw new NotYetConnectedException();
        }
        return childChannelContext.getChannel().pipeline().writeAndFlush(msg);
    }

    private synchronized void start() {
        if (!handshakeFuture.isDone()) {
            startHandshakeTimeout();
        }
    }

    public synchronized void onProcessDead() {
        if (this.childChannelContext == null) {
            handshakeFailure(new SipcProcessDeadException());
        } else {
            remove(null);
        }
    }

    public synchronized void onProcessDead(Throwable e) {
        if (this.childChannelContext == null) {
            handshakeFailure(new SipcProcessDeadException(e));
        } else {
            remove(e);
        }
    }

    private synchronized void onHandshakeTimeout() {
        handshakeFailure(new SipcHandshakeTimeoutException());
    }

    private synchronized void handshakeFailure(Throwable cause) {
        try {
            if (this.childChannelContext != null) {
                return;
            }

            if (handshakeTimeoutFuture != null) {
                handshakeTimeoutFuture.cancel(false);
            }

            if (!handshakeFuture.isDone()) {
                handshakeFuture.completeExceptionally(cause);
            }

            if (this.channelHandler instanceof SipcServerChannelHandler) {
                ((SipcServerChannelHandler) this.channelHandler).onHandshakeFailed(this, cause);
            }
        } finally {
            remove(cause);
        }
    }

    private synchronized void remove(@Nullable Throwable cause) {
        parent.serverContext.removeChild(connectInfo.getConnectionId());

        if (this.channelHandler instanceof SipcServerChannelHandler) {
            ((SipcServerChannelHandler) this.channelHandler).onRemoved(this, cause);
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
