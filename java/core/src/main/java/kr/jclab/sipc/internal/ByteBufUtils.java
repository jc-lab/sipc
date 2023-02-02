package kr.jclab.sipc.internal;

import io.netty.buffer.ByteBuf;

public class ByteBufUtils {
    public static byte[] toByteArray(ByteBuf buf) {
        byte[] byteArray = new byte[buf.readableBytes()];
        buf.slice().readBytes(byteArray);
        return byteArray;
    }
}
