package kr.jclab.javautils.sipc.crypto;

public interface EphemeralKeyPairGenerator {
    String getAlgorithm();
    EphemeralKeyPair generate() throws CryptoException;
}
