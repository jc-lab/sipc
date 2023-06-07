package kr.jclab.sipc.server.internal;

import io.netty.channel.Channel;
import kr.jclab.sipc.internal.InvalidConnectionInfoException;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.SipcChild;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Getter
public class SipcChildChannelContext {
    private final SipcServerContext serverContext;
    private final Channel channel;
    private final int pid;

    private HandshakeState state = HandshakeState.HANDSHAKEING_1;
    private String connectionId = null;
    private SipcChild sipcChild = null;
    private boolean verified = false;

    public enum HandshakeState {
        HANDSHAKEING_1,
        HANDSHAKEING_2,
        HANDSHAKED,
        CLOSED
    }

    public SipcChildChannelContext(SipcServerContext serverContext, Channel channel, int pid) {
        this.serverContext = serverContext;
        this.channel = channel;
        this.pid = pid;
    }

    public CompletableFuture<Boolean> onClientHello(byte[] rawPayload) {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();
        try {
            SipcProto.ClientHelloPayload payload = SipcProto.ClientHelloPayload.newBuilder()
                    .mergeFrom(rawPayload)
                    .build();

            checkState(this.connectionId == null);
            SipcChild sipcChild = serverContext.getSipcChild(payload.getConnectionId());
            if (sipcChild == null) {
                completableFuture.completeExceptionally(new InvalidConnectionInfoException());
                return completableFuture;
            }

            this.sipcChild = sipcChild;
            this.connectionId = payload.getConnectionId();
            this.state = SipcChildChannelContext.HandshakeState.HANDSHAKEING_2;

            int expectedPid = this.sipcChild.getPid().get();
            if (expectedPid != 0) {
                checkPid(completableFuture, expectedPid);
            } else {
                this.sipcChild.getPid().compute((updatedPid) -> {
                    checkPid(completableFuture, updatedPid);
                });
            }
            return completableFuture;
        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    private void checkPid(CompletableFuture<Boolean> completableFuture, int expectedPid) {
        if (this.serverContext.isDisablePidCheck() || expectedPid == pid) {
            this.verified = true;
            try {
                completableFuture.complete(true);
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        } else {
            completableFuture.completeExceptionally(new InvalidConnectionInfoException());
        }
    }

    public void noiseHandshakeComplete(Channel channel) {
        checkNotNull(this.connectionId);
        checkNotNull(this.sipcChild);
        checkState(this.verified);

        this.sipcChild.internalAttachChannel(this);
        channel.pipeline().addLast(this.sipcChild.getChannelHandler());
    }

    public void onHandshakeFailure() {
        this.state = HandshakeState.CLOSED;
    }
}
