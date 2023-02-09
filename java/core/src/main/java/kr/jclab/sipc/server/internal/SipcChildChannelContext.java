package kr.jclab.sipc.server.internal;

import io.netty.channel.Channel;
import io.netty.channel.unix.PeerCredentials;
import kr.jclab.sipc.internal.InvalidConnectionInfoException;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.SipcChild;
import lombok.Getter;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

@Getter
public class SipcChildChannelContext {
    private final SipcServerContext serverContext;
    private final Channel channel;
    private final int pid;

    public HandshakeState state = HandshakeState.HANDSHAKEING_1;
    private String connectionId = null;
    private SipcChild sipcChild = null;

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

    public void onClientHello(SipcProto.ClientHelloPayload payload) {
        checkState(this.connectionId == null);
        SipcChild sipcChild = serverContext.getSipcChild(payload.getConnectionId());
        if (sipcChild == null) {
            throw new InvalidConnectionInfoException();
        }
        if (sipcChild.getPid() != pid) {
            throw new InvalidConnectionInfoException();
        }
        this.sipcChild = sipcChild;
        this.connectionId = payload.getConnectionId();
        this.state = SipcChildChannelContext.HandshakeState.HANDSHAKEING_2;
    }

    public void onHandshakeComplete() {
        checkNotNull(this.connectionId);
        checkNotNull(this.sipcChild);

        this.sipcChild.internalAttachChannel(this);
    }
}
