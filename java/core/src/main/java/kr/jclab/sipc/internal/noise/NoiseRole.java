package kr.jclab.sipc.internal.noise;

import kr.jclab.noise.protocol.HandshakeState;

public enum NoiseRole {
    INITIATOR(HandshakeState.INITIATOR),
    RESPONDER(HandshakeState.RESPONDER)
    ;

    private final int value;

    NoiseRole(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
