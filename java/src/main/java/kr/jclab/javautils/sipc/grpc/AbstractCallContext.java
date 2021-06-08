package kr.jclab.javautils.sipc.grpc;

import kr.jclab.sipc.common.proto.Frames;

public abstract class AbstractCallContext {
    public abstract void onRecvMessage(Frames.GrpcMessage payload);
    public abstract void onRecvHalfClose(Frames.GrpcHalfClose payload);
    public abstract void onRecvHeaders(Frames.GrpcHeaders payload);
    public abstract void onRecvComplete(Frames.GrpcComplete payload);
}
