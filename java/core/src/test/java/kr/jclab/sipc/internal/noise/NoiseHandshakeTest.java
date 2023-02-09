package kr.jclab.sipc.internal.noise;

import kr.jclab.noise.protocol.CipherStatePair;
import kr.jclab.noise.protocol.DHState;
import kr.jclab.noise.protocol.HandshakeState;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.crypto.BadPaddingException;
import javax.crypto.ShortBufferException;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NoiseHandshakeTest {
    private HandshakeState aliceHS;
    private HandshakeState bobHS;
    
    @Test
    @Order(1)
    public void setUpKeys() throws NoSuchAlgorithmException {
        // test1
        // Noise framework initialization
        aliceHS = new HandshakeState("Noise_IK_25519_ChaChaPoly_SHA256", HandshakeState.INITIATOR);
        bobHS = new HandshakeState("Noise_IK_25519_ChaChaPoly_SHA256", HandshakeState.RESPONDER);

        assertNotNull(aliceHS);
        assertNotNull(bobHS);

        if (aliceHS.needsLocalKeyPair()) {
            DHState localKeyPair = aliceHS.getLocalKeyPair();
            localKeyPair.generateKeyPair();

            byte[] prk = new byte[localKeyPair.getPrivateKeyLength()];
            byte[] puk = new byte[localKeyPair.getPublicKeyLength()];
            localKeyPair.getPrivateKey(prk, 0);
            localKeyPair.getPublicKey(puk, 0);

            assert(aliceHS.hasLocalKeyPair());
        }

        if (bobHS.needsLocalKeyPair()) {
            bobHS.getLocalKeyPair().generateKeyPair();
        }

        if (aliceHS.needsRemotePublicKey() || bobHS.needsRemotePublicKey()) {
            aliceHS.getRemotePublicKey().copyFrom(bobHS.getLocalKeyPair());
            bobHS.getRemotePublicKey().copyFrom(aliceHS.getLocalKeyPair());

            assert(aliceHS.hasRemotePublicKey());
            assert(bobHS.hasRemotePublicKey());
        }
    }

    @Test
    @Order(2)
    public void startHandshake() throws NoSuchAlgorithmException {
        setUpKeys();

        // protocol starts and respective resulting state
        aliceHS.start();
        bobHS.start();

        assert(aliceHS.getAction() != HandshakeState.FAILED);
        assert(bobHS.getAction() != HandshakeState.FAILED);
    }

    @Test
    @Order(3)
    public void completeHandshake() throws ShortBufferException, BadPaddingException, NoSuchAlgorithmException {
        startHandshake();
        // - creation of initiator ephemeral key

        // ephemeral keys become part of the Noise protocol instance
        // Noise currently hides this part of the protocol
        // test by testing later parts of the protocol

        // after a successful communication of responder information
        // need to construct DH parameters of form se and ee

        byte[] aliceSendBuffer = new byte[65535];
        int aliceMsgLength = 0;

        byte[] bobSendBuffer = new byte[65535];
        int bobMsgLength = 0;

        byte[] payload = new byte[65535];

        aliceMsgLength = aliceHS.writeMessage(aliceSendBuffer, 0, payload, 0, 0);
        bobHS.readMessage(aliceSendBuffer, 0, aliceMsgLength, payload, 0);
        bobMsgLength = bobHS.writeMessage(bobSendBuffer, 0, payload, 0, 0);
        aliceHS.readMessage(bobSendBuffer, 0, bobMsgLength, payload, 0);

        // at split state
        CipherStatePair aliceSplit = aliceHS.split();
        CipherStatePair bobSplit = bobHS.split();

        byte[] acipher = new byte[65535];
        int acipherLength = 0;
        byte[] bcipher = new byte[65535];
        int bcipherLength = 0;
        String s1 = "Hello World!";
        acipherLength = aliceSplit.getSender().encryptWithAd(null, s1.getBytes(), 0, acipher, 0, s1.length());
        bcipherLength = bobSplit.getReceiver().decryptWithAd(null, acipher, 0, bcipher, 0, acipherLength);

        assertArrayEquals(s1.getBytes(), Arrays.copyOf(bcipher, bcipherLength));
        assert(aliceHS.getAction() == HandshakeState.COMPLETE);
        assert(bobHS.getAction() == HandshakeState.COMPLETE);
    }
}