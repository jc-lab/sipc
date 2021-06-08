package kr.jclab.javautils.sipc.grpc;

import kr.jclab.sipc.common.proto.Frames;

public class ServerCallContext extends AbstractCallContext {
    private final ServerCallImpl<?, ?> serverCall;

    public ServerCallContext(ServerCallImpl<?, ?> serverCall) {
        this.serverCall = serverCall;
    }

    public ServerCallImpl<?, ?> getServerCall() {
        return serverCall;
    }

    @Override
    public void onRecvMessage(Frames.GrpcMessage payload) {
        this.serverCall.onRecvMessage(payload);
    }

    @Override
    public void onRecvHalfClose(Frames.GrpcHalfClose payload) {
        this.serverCall.onRecvHalfClose(payload);
    }

    @Override
    public void onRecvHeaders(Frames.GrpcHeaders payload) {

    }

    @Override
    public void onRecvComplete(Frames.GrpcComplete payload) {
        this.serverCall.onRecvComplete(payload);
    }
}
