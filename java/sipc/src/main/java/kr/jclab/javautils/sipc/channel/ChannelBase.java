package kr.jclab.javautils.sipc.channel;

import java.nio.ByteBuffer;

public interface ChannelBase {
    void write(ByteBuffer data);
}
