import kr.jclab.javautils.sipc.crypto.CryptoException;
import kr.jclab.javautils.sipc.crypto.DefaultEphemeralKeyAlgorithmsFactory;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPairGenerator;
import kr.jclab.javautils.sipc.crypto.x25519.X25519KeyPairGenerator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class CryptoTest {
    private void autoDeriveTest(EphemeralKeyPairGenerator keyPairGenerator) throws CryptoException {
        EphemeralKeyPair kp1 = keyPairGenerator.generate();
        EphemeralKeyPair kp2 = keyPairGenerator.generate();

        byte[] dk1 = kp1.derive(kp2.getPublicKey());
        byte[] dk2 = kp2.derive(kp1.getPublicKey());

        assert Arrays.equals(dk1, dk2);
    }

    @Test
    public void shouldPassDeriveByDefaultAlgorithm() throws CryptoException {
        EphemeralKeyPairGenerator keyPairGenerator = DefaultEphemeralKeyAlgorithmsFactory.getInstance().getHostKeyPairGenerator();
        autoDeriveTest(keyPairGenerator);
    }

    @Test
    public void shouldPassDeriveByX25519() throws CryptoException {
        X25519KeyPairGenerator keyPairGenerator = new X25519KeyPairGenerator();

        assert "x25519".equals(keyPairGenerator.getAlgorithm());

        autoDeriveTest(keyPairGenerator);
    }

    @Test
    public void shouldPassFindX25519() {
        assert DefaultEphemeralKeyAlgorithmsFactory.getInstance().getKeyPairGenerator("x25519") != null;
    }

    @Test
    public void shouldPassInvalidAlgorithmToNull() {
        assert DefaultEphemeralKeyAlgorithmsFactory.getInstance().getKeyPairGenerator("abcd") == null;
    }
}
