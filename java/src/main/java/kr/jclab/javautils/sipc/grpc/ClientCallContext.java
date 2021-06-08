package kr.jclab.javautils.sipc.grpc;

import io.grpc.Metadata;
import kr.jclab.sipc.common.proto.Frames;

public class ClientCallContext extends AbstractCallContext {
    private final ClientCallImpl<?, ?> clientCall;

    public ClientCallContext(ClientCallImpl<?, ?> clientCall) {
        this.clientCall = clientCall;
    }

    public ClientCallImpl<?, ?> getClientCall() {
        return clientCall;
    }

    @Override
    public void onRecvMessage(Frames.GrpcMessage payload) {
        this.clientCall.onRecvMessage(payload);
    }

    @Override
    public void onRecvHalfClose(Frames.GrpcHalfClose payload) {
    }

    @Override
    public void onRecvHeaders(Frames.GrpcHeaders payload) {
        Metadata headers = MetadataHelper.parse(payload.getHeaders());
        this.clientCall.onRecvHeaders(headers);
    }

    @Override
    public void onRecvComplete(Frames.GrpcComplete payload) {
        this.clientCall.onRecvComplete(payload);
    }
}
