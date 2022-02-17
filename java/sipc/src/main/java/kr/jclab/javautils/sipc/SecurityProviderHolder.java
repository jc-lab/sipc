package kr.jclab.javautils.sipc;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.security.SecureRandom;

public class SecurityProviderHolder {
    public static Provider PROVIDER = new BouncyCastleProvider();
    public static SecureRandom SECURE_RANDOM = new SecureRandom();
}
