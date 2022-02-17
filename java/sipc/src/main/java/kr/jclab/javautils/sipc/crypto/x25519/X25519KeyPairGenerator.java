package kr.jclab.javautils.sipc.crypto.x25519;

import kr.jclab.javautils.sipc.crypto.CryptoException;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPairGenerator;

public class X25519KeyPairGenerator implements EphemeralKeyPairGenerator {
    @Override
    public String getAlgorithm() {
        return X25519KeyPair.ALGORITHM;
    }

    @Override
    public EphemeralKeyPair generate() throws CryptoException {
        return new X25519KeyPair();
    }
}
