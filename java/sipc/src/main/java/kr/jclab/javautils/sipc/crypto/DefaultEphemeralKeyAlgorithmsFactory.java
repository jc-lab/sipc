package kr.jclab.javautils.sipc.crypto;

import kr.jclab.javautils.sipc.crypto.x25519.X25519KeyPair;
import kr.jclab.javautils.sipc.crypto.x25519.X25519KeyPairGenerator;

import java.util.*;

public class DefaultEphemeralKeyAlgorithmsFactory implements EphemeralKeyAlgorithmFactory {
    private final Map<String, EphemeralKeyPairGenerator> algorithms;

    public static DefaultEphemeralKeyAlgorithmsFactory getInstance() {
        return LazyHolder.INSTANCE;
    }

    public static class LazyHolder {
        public static final DefaultEphemeralKeyAlgorithmsFactory INSTANCE = new DefaultEphemeralKeyAlgorithmsFactory();
    }

    private DefaultEphemeralKeyAlgorithmsFactory() {
        X25519KeyPairGenerator x25519 = new X25519KeyPairGenerator();

        this.algorithms = Collections.unmodifiableMap(
                new HashMap<String, EphemeralKeyPairGenerator>() {{
                    put(x25519.getAlgorithm(), x25519);
                }}
        );
    }

    @Override
    public List<String> getAlgorithms() {
        return new ArrayList<>(this.algorithms.keySet());
    }

    @Override
    public EphemeralKeyPairGenerator getKeyPairGenerator(String algorithm) {
        return this.algorithms.get(algorithm);
    }

    @Override
    public EphemeralKeyPairGenerator getHostKeyPairGenerator() {
        return this.getKeyPairGenerator(X25519KeyPair.ALGORITHM);
    }
}
