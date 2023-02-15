package kr.jclab.sipc.internal.noise;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import kr.jclab.noise.protocol.CipherState;
import kr.jclab.sipc.internal.ByteBufUtils;
import kr.jclab.sipc.internal.CantDecryptInboundException;
import kr.jclab.sipc.internal.SecureChannelError;
import kr.jclab.sipc.internal.SipcUtils;
import lombok.extern.slf4j.Slf4j;

import java.security.GeneralSecurityException;
import java.util.List;

@Slf4j
public class NoiseCipherCodec extends MessageToMessageCodec<ByteBuf, ByteBuf> {
    public static final String HANDLER_NAME = "noiseCipherCodec";

    private final CipherState aliceCipher;
    private final CipherState bobCipher;

    private boolean abruptlyClosing = false;

    public NoiseCipherCodec(CipherState aliceCipher, CipherState bobCipher) {
        this.aliceCipher = aliceCipher;
        this.bobCipher = bobCipher;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        int plainLength = msg.readableBytes();
        byte[] buf = new byte[plainLength + aliceCipher.getMACLength()];
        msg.readBytes(buf, 0, plainLength);
        int length = aliceCipher.encryptWithAd(null, buf, 0, buf, 0, plainLength);
        out.add(Unpooled.wrappedBuffer(buf, 0, length));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
        if (abruptlyClosing) {
            // if abrupt close was initiated by our node we shouldn't try decoding anything else
            return ;
        }
        byte[] buf = ByteBufUtils.toByteArray(msg);
        int decryptLen;
        try {
            decryptLen = bobCipher.decryptWithAd(null, buf, 0, buf, 0, buf.length);
        } catch (GeneralSecurityException e) {
            throw new CantDecryptInboundException("Unable to decrypt a message from remote", e);
        }
        out.add(Unpooled.wrappedBuffer(buf, 0, decryptLen));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (SipcUtils.hasCauseOfType(cause, SecureChannelError.class)) {
            log.debug("Invalid Noise content", cause);
            closeAbruptly(ctx);
        } else {
            super.exceptionCaught(ctx, cause);
        }
    }

    private void closeAbruptly(ChannelHandlerContext ctx) {
        abruptlyClosing = true;
        ctx.close();
    }
}
