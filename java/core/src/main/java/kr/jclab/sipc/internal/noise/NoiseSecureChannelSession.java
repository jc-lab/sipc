package kr.jclab.sipc.internal.noise;

import kr.jclab.noise.protocol.CipherState;
import lombok.Data;
import lombok.Getter;

@Getter
public class NoiseSecureChannelSession {
    private final CipherState aliceCipher;
    private final CipherState bobCipher;

    public NoiseSecureChannelSession(CipherState aliceCipher, CipherState bobCipher) {
        this.aliceCipher = aliceCipher;
        this.bobCipher = bobCipher;
    }
}
