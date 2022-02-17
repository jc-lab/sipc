package kr.jclab.javautils.sipc.crypto;

public interface EphemeralKeyPair {
    /**
     * get algorithm name
     *
     * @return algorithm name
     */
    String getAlgorithm();

    /**
     * derive shared key
     *
     * @param publicKeyBinary peer public key
     * @return shared key
     * @throws CryptoException exception
     */
    byte[] derive(byte[] publicKeyBinary) throws CryptoException;

    /**
     * get public key
     *
     * @return X509 Encoded Public Key
     */
    byte[] getPublicKey();
}
