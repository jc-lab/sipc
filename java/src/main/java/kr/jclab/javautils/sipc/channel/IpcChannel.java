package kr.jclab.javautils.sipc.channel;

import com.google.protobuf.GeneratedMessageV3;
import kr.jclab.javautils.sipc.bson.entity.ConnectInfo;
import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;

/**
 * Per Channel
 */
public interface IpcChannel extends FrameConverter.FrameHandlers {
    String getChannelId();
    ConnectInfo getConnectInfo();
    void attachClientContext(ClientContext clientContext);
    IpcChannelStatus getChannelStatus();
    void sendFrame(GeneratedMessageV3 frame) throws IOException;
    void sendWrappedData(Frames.WrappedData wrappedData) throws IOException;
    <T extends GeneratedMessageV3> void registerWrappedData(T messageDefaultInstance, WrappedDataReceiver<T> receiver);
    void addCleanupHandler(CleanupHandler cleanupHandler);
}
