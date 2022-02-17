package kr.jclab.javautils.sipc.crypto.x25519;

import kr.jclab.javautils.sipc.SecurityProviderHolder;
import kr.jclab.javautils.sipc.crypto.CryptoException;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;
import org.bouncycastle.jcajce.spec.XDHParameterSpec;

import javax.crypto.KeyAgreement;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class X25519KeyPair implements EphemeralKeyPair {
    public static final String ALGORITHM = "x25519";

    private final String paramSpec = XDHParameterSpec.X25519;
    private final KeyPair keyPair;

    public X25519KeyPair() throws CryptoException {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("XDH", SecurityProviderHolder.PROVIDER);
            kpg.initialize(new XDHParameterSpec(this.paramSpec));
            this.keyPair = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public String getAlgorithm() {
        return ALGORITHM;
    }

    @Override
    public byte[] derive(byte[] publicKeyBinary) throws CryptoException {
        try {
            KeyAgreement ka = KeyAgreement.getInstance("XDH", SecurityProviderHolder.PROVIDER);
            ka.init(this.keyPair.getPrivate());

            KeyFactory pkf = KeyFactory.getInstance("XDH", SecurityProviderHolder.PROVIDER);
            PublicKey publicKey = pkf.generatePublic(new X509EncodedKeySpec(publicKeyBinary));
            ka.doPhase(publicKey, true);

            return ka.generateSecret();
        } catch (NoSuchAlgorithmException | InvalidKeyException | InvalidKeySpecException e) {
            throw new CryptoException(e);
        }
    }

    @Override
    public byte[] getPublicKey() {
        return this.keyPair.getPublic().getEncoded();
    }

    @Override
    public String toString() {
        return toHex(this.keyPair.getPrivate().getEncoded());
    }

    public static String toHex(byte[] data) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : data) {
            stringBuilder.append(String.format("%02x ", b));
        }
        return stringBuilder.toString();
    }
}
