package kr.jclab.javautils.sipc.crypto;

import java.util.List;

public interface EphemeralKeyAlgorithmFactory {
    List<String> getAlgorithms();
    EphemeralKeyPairGenerator getKeyPairGenerator(String algorithm);
    EphemeralKeyPairGenerator getHostKeyPairGenerator();
}
