package kr.jclab.javautils.sipc.channel;

import com.google.protobuf.GeneratedMessageV3;
import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;

@FunctionalInterface
public interface WrappedDataReceiver<T extends GeneratedMessageV3> {
    void onMessage(Frames.WrappedData wrappedData, T decodedMessage) throws IOException;
}
