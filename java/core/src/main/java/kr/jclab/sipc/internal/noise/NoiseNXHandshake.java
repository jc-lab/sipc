package kr.jclab.sipc.internal.noise;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import kr.jclab.noise.protocol.*;
import kr.jclab.sipc.internal.ByteBufUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.BadPaddingException;
import javax.crypto.ShortBufferException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

@Slf4j
public class NoiseNXHandshake extends SimpleChannelInboundHandler<ByteBuf> {
    public static final String HANDLER_NAME = "noiseHandshake";

    private final NoiseRole role;
    private final NoiseHandler noiseHandler;

    @Getter
    private final HandshakeState handshakeState;
    private boolean activated = false;

    public NoiseNXHandshake(NoiseRole role, NoiseHandler noiseHandler) throws NoSuchAlgorithmException {
        this.role = role;
        this.noiseHandler = noiseHandler;
        this.handshakeState = new HandshakeState("Noise_NX_25519_ChaChaPoly_SHA256", role.getValue());
    }

    public void start() {
        this.handshakeState.start();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!this.activated) {
            this.activated = true;
            if (this.role == NoiseRole.INITIATOR) {
                sendNoiseMessage(ctx, this.noiseHandler.getInitiatorMessage());
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        channelActive(ctx);

        // we always read from the wire when it's the next action to take
        // capture any payloads
        if (handshakeState.getAction() == HandshakeState.READ_MESSAGE) {
            byte[] payload = readNoiseMessage(ByteBufUtils.toByteArray(msg));
            noiseHandler.onReadMessage(this, payload);
        }

//        // verify the signature of the remote's noise static public key once
//        // the remote public key has been provided by the XX protocol
//        DHState derivedRemotePublicKey = handshakeState.getRemotePublicKey();
//        if (derivedRemotePublicKey.hasPublicKey()) {
//            remotePeerId = verifyPayload(ctx, instancePayload!!, derivedRemotePublicKey)
//            if (role == NoiseRole.INIT && expectedRemotePeerId != remotePeerId) {
//                throw InvalidRemotePubKey()
//            }
//        }

        // after reading messages and setting up state, write next message onto the wire
        if (handshakeState.getAction() == HandshakeState.WRITE_MESSAGE) {
            sendNoiseMessage(ctx, null);
        }

        if (handshakeState.getAction() == HandshakeState.SPLIT) {
            splitHandshake(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handshakeFailed(ctx, cause);
        ctx.channel().close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        handshakeFailed(ctx, "Connection was closed ${ctx.channel()}");
        super.channelUnregistered(ctx);
    }

    private byte[] readNoiseMessage(byte[] msg) throws ShortBufferException, BadPaddingException {
        log.debug("Noise handshake READ_MESSAGE");

        byte[] payload = new byte[msg.length];
        int payloadLength = handshakeState.readMessage(msg, 0, msg.length, payload, 0);

        log.trace("msg.size:" + msg.length);
        log.trace("Read message size:$payloadLength");

        if (payloadLength > 0) {
            return Arrays.copyOf(payload, payloadLength);
        }

        return null;
    }

//    private void sendNoiseStaticKeyAsPayload(ChannelHandlerContext ctx) {
//        // only send the Noise static key once
//        if (sentNoiseKeyPayload) return
//                sentNoiseKeyPayload = true
//
//        // the payload consists of the identity public key, and the signature of the noise static public key
//        // the actual noise static public key is sent later as part of the XX handshake
//
//        // get identity public key
//        val identityPublicKey: ByteArray = marshalPublicKey(localKey.publicKey())
//
//        // get noise static public key signature
//        val localNoiseStaticKeySignature =
//                localKey.sign(noiseSignaturePhrase(localNoiseState))
//
//        // generate an appropriate protobuf element
//        val noiseHandshakePayload =
//                Spipe.NoiseHandshakePayload.newBuilder()
//                        .setLibp2PKey(ByteString.copyFrom(identityPublicKey))
//                        .setNoiseStaticKeySignature(ByteString.copyFrom(localNoiseStaticKeySignature))
//                        .setLibp2PData(ByteString.EMPTY)
//                        .setLibp2PDataSignature(ByteString.EMPTY)
//                        .build()
//                        .toByteArray()
//
//        // create the message with the signed payload -
//        // verification happens once the noise static key is shared
//        log.debug("Sending signed Noise static public key as payload")
//        sendNoiseMessage(ctx, noiseHandshakePayload)
//    }

    private void sendNoiseMessage(ChannelHandlerContext ctx, byte[] msg) throws ShortBufferException {
        int msgLength = (msg != null) ? msg.length : 0;
        int localKeyPairSize = handshakeState.hasLocalKeyPair() ? handshakeState.getLocalKeyPair().getPrivateKeyLength() : 0;
        byte[] outputBuffer = new byte[msgLength + (2 * (localKeyPairSize + 16))]; // 16 is MAC length
        int outputLength = handshakeState.writeMessage(outputBuffer, 0, msg, 0, msgLength);

        log.debug("Noise handshake WRITE_MESSAGE");
        log.trace("Sent message length:" + outputLength);

        ctx.writeAndFlush(Unpooled.wrappedBuffer(Arrays.copyOfRange(outputBuffer, 0, outputLength)));
    }

//    private void verifyPayload(
//            ChannelHandlerContext ctx,
//            byte[] payload,
//            DHState remotePublicKeyState
//    ) {
//
//    }

//    private Pair<PubKey, ByteArray> unpackKeyAndSignature(byte[] payload) {
//        val noiseMsg = Spipe.NoiseHandshakePayload.parseFrom(payload)
//
//        val publicKey = unmarshalPublicKey(noiseMsg.libp2PKey.toByteArray())
//        val signature = noiseMsg.noiseStaticKeySignature.toByteArray()
//
//        return Pair(publicKey, signature)
//    }

    private void splitHandshake(ChannelHandlerContext ctx) {
        CipherStatePair cipherStatePair = handshakeState.split();

        CipherState aliceSplit = cipherStatePair.getSender();
        CipherState bobSplit = cipherStatePair.getReceiver();
        log.debug("Split complete");

        // put alice and bob security sessions into the context and trigger the next action
        NoiseSecureChannelSession secureSession = new NoiseSecureChannelSession(
                aliceSplit,
                bobSplit
        );

        handshakeSucceeded(ctx, secureSession);
    }

    private void handshakeSucceeded(ChannelHandlerContext ctx, NoiseSecureChannelSession session) {
        ctx.pipeline()
                .addBefore(
                        HANDLER_NAME,
                        NoiseCipherCodec.HANDLER_NAME,
                        new NoiseCipherCodec(session.getAliceCipher(), session.getBobCipher())
                );
        ctx.pipeline().remove(this);

        noiseHandler.onHandshakeComplete(this, session);

        ctx.fireChannelActive();
    }

    private void handshakeFailed(ChannelHandlerContext ctx, String cause) {
        handshakeFailed(ctx, new Exception(cause));
    }

    private void handshakeFailed(ChannelHandlerContext ctx, Throwable cause) {
        noiseHandler.onHandshakeFailed(this, cause);
        log.warn("Noise handshake failed", cause);
        ctx.pipeline().remove(this);
    }
}
