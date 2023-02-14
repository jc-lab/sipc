package kr.jclab.sipc.internal;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;

import java.nio.ByteOrder;

public class PacketCoder {
    public static class Encoder extends LengthFieldPrepender {
        public Encoder() {
            super(ByteOrder.BIG_ENDIAN, 2, 0, false);
        }
    }

    public static class Decoder extends LengthFieldBasedFrameDecoder {
        public Decoder() {
            super(ByteOrder.BIG_ENDIAN, 65535, 0, 2, 0, 2, true);
        }
    }
}
