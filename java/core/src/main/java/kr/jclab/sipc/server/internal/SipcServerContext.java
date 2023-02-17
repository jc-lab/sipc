package kr.jclab.sipc.server.internal;

import kr.jclab.noise.protocol.DHState;
import kr.jclab.noise.protocol.Noise;
import kr.jclab.sipc.OsDetector;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.internal.WindowsJnaSupport;
import kr.jclab.sipc.platform.WindowsNativeSupport;
import kr.jclab.sipc.proto.SipcProto;
import kr.jclab.sipc.server.SipcChild;
import lombok.Getter;
import lombok.Setter;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

public class SipcServerContext {
    @Getter
    private final EventLoopHolder eventLoopHolder;
    @Getter
    private final SipcProto.TransportType transportType;

    @Getter
    private final DHState localPrivateKey;
    @Getter
    private final WindowsNativeSupport windowsNativeSupport;

    private final ConcurrentHashMap<String, SipcChild> childMapByConnectionId = new ConcurrentHashMap<>();

    @Getter
    private int handshakeTimeout = 30000;
    @Getter
    @Setter
    private boolean allowReconnect = false;

    private ScheduledFuture<?> handshakeTimer = null;

    public SipcServerContext(
            EventLoopHolder eventLoopHolder,
            DHState localPrivateKey,
            SipcProto.TransportType transportType,
            WindowsNativeSupport windowsNativeSupport
    ) throws NoSuchAlgorithmException {
        this.eventLoopHolder = eventLoopHolder;
        if (localPrivateKey == null) {
            localPrivateKey = Noise.createDH("25519");
            localPrivateKey.generateKeyPair();
        }
        this.localPrivateKey = localPrivateKey;
        this.transportType = transportType;
        if (windowsNativeSupport != null) {
            this.windowsNativeSupport = windowsNativeSupport;
        } else if (OsDetector.IS_WINDOWS) {
            this.windowsNativeSupport = new WindowsJnaSupport();
        } else {
            this.windowsNativeSupport = null;
        }
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

    public void addChild(String connectionId, SipcChild child) {
        childMapByConnectionId.put(connectionId, child);
    }

    public boolean removeChild(String connectionId) {
        return childMapByConnectionId.remove(connectionId) != null;
    }
}
