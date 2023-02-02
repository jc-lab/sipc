package kr.jclab.sipc.server.internal;

import kr.jclab.noise.protocol.DHState;
import kr.jclab.noise.protocol.Noise;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.internal.NotifyMap;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.SipcChild;
import lombok.Getter;
import lombok.Setter;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
public class SipcServerContext {
    private final EventLoopHolder eventLoopHolder;
    private final SipcProto.TransportType transportType;
    private final String transportAddress;

    private final DHState localPrivateKey;
    private final NotifyMap<String, SipcChild> childMapByConnectionId = new NotifyMap<>();

    @Getter
    private int handshakeTimeout = 30000;
    private ScheduledFuture<?> handshakeTimer = null;

    public SipcServerContext(
            EventLoopHolder eventLoopHolder,
            DHState localPrivateKey,
            SipcProto.TransportType transportType,
            String transportAddress
    ) throws NoSuchAlgorithmException {
        this.eventLoopHolder = eventLoopHolder;
        if (localPrivateKey == null) {
            localPrivateKey = Noise.createDH("25519");
            localPrivateKey.generateKeyPair();
        }
        this.localPrivateKey = localPrivateKey;
        this.transportType = transportType;
        this.transportAddress = transportAddress;
    }

    public void shutdown() {
        eventLoopHolder.shutdown();
    }

    public void setHandshakeTimeout(int handshakeTimeout) {
        this.handshakeTimeout = handshakeTimeout;
    }

    public byte[] getLocalPublicKey() {
        byte[] publicKey = new byte[32];
        localPrivateKey.getPublicKey(publicKey, 0);
        return publicKey;
    }

    public SipcChild getSipcChild(String connectionId) {
        SipcChild sipcChild = childMapByConnectionId.get(connectionId);
        if (sipcChild == null) {
            return null;
        }
        return sipcChild;
    }

    private void onHandshakeTimeout() {

    }
}
