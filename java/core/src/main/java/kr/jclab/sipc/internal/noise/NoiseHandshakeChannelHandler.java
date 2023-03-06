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
import java.util.concurrent.CompletableFuture;

@Slf4j
public class NoiseHandshakeChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {
    public static final String HANDLER_NAME = "noiseHandshake";

    private final NoiseRole role;
    private final NoiseHandler noiseHandler;

    @Getter
    private final HandshakeState handshakeState;
    private boolean activated = false;

    public NoiseHandshakeChannelHandler(NoiseRole role, NoiseHandler noiseHandler) throws NoSuchAlgorithmException {
        this.role = role;
        this.noiseHandler = noiseHandler;
        this.handshakeState = new HandshakeState("Noise_XK_25519_ChaChaPoly_SHA256", role.getValue());
    }

    public void start() {
        this.handshakeState.start();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (!this.activated) {
            this.activated = true;
            if (this.role == NoiseRole.INITIATOR) {
                sendNoiseMessage(ctx, this.noiseHandler.beforeWriteMessage(this));
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        channelActive(ctx);

        CompletableFuture<Boolean> completableFuture;

        // we always read from the wire when it's the next action to take
        // capture any payloads
        if (handshakeState.getAction() == HandshakeState.READ_MESSAGE) {
            byte[] payload = readNoiseMessage(ByteBufUtils.toByteArray(msg));
            completableFuture = noiseHandler.onReadMessage(this, payload);
        } else {
            completableFuture = new CompletableFuture<>();
            completableFuture.complete(true);
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

        completableFuture
                .whenComplete((accept, ex) -> {
                    if (ex != null) {
                        ctx.fireExceptionCaught(ex);
                        return ;
                    }
                    if (!accept) {
                        ctx.fireExceptionCaught(new RuntimeException("handshake rejected"));
                        return ;
                    }

                    // after reading messages and setting up state, write next message onto the wire
                    if (handshakeState.getAction() == HandshakeState.WRITE_MESSAGE) {
                        if (!sendNoiseMessage(ctx, this.noiseHandler.beforeWriteMessage(this))) {
                            return ;
                        }
                    }

                    if (handshakeState.getAction() == HandshakeState.SPLIT) {
                        splitHandshake(ctx);
                    }
                });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        handshakeFailed(ctx, cause);
        ctx.channel().close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        handshakeFailed(ctx, "Connection was closed " + ctx.channel());
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

    private boolean sendNoiseMessage(ChannelHandlerContext ctx, byte[] msg) {
        int msgLength = (msg != null) ? msg.length : 0;
        int localKeyPairSize = handshakeState.hasLocalKeyPair() ? handshakeState.getLocalKeyPair().getPrivateKeyLength() : 0;
        byte[] outputBuffer = new byte[msgLength + (2 * (localKeyPairSize + 16))]; // 16 is MAC length
        int outputLength = 0;
        try {
            outputLength = handshakeState.writeMessage(outputBuffer, 0, msg, 0, msgLength);

            log.debug("Noise handshake WRITE_MESSAGE");
            log.trace("Sent message length:" + outputLength);

            ctx.writeAndFlush(Unpooled.wrappedBuffer(Arrays.copyOfRange(outputBuffer, 0, outputLength)));
            return true;
        } catch (ShortBufferException e) {
            ctx.fireExceptionCaught(e);
        }
        return false;
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
                .addAfter(
                        ctx.name(),
                        NoiseCipherCodec.HANDLER_NAME,
                        new NoiseCipherCodec(session.getAliceCipher(), session.getBobCipher())
                );
        ctx.pipeline().remove(this);

        try {
            noiseHandler.onHandshakeComplete(this, session);
            ctx.fireChannelActive();
        } catch (Exception e) {
            ctx.fireExceptionCaught(e);
        }
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
