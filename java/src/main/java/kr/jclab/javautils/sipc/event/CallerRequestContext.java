package kr.jclab.javautils.sipc.event;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class CallerRequestContext<TRES extends GeneratedMessageV3, TPROG extends GeneratedMessageV3> extends RequestContext {
    private final TRES responseDefaultInstance;
    private final TPROG progressDefaultInstance;
    private final EventChannel.ProtoHandler<TPROG> progressHandler;

    private final CompletableFuture<TRES> completableFuture = new CompletableFuture<>();

    public CallerRequestContext(
            EventChannel eventChannel,
            String streamId,
            TRES responseDefaultInstance,
            TPROG progressDefaultInstance,
            EventChannel.ProtoHandler<TPROG> progressHandler
            ) {
        super(eventChannel, streamId);
        this.responseDefaultInstance = responseDefaultInstance;
        this.progressDefaultInstance = progressDefaultInstance;
        this.progressHandler = progressHandler;
    }

    public Future<TRES> getFuture() {
        return this.completableFuture;
    }

    @Override
    public void onProgressMessage(Frames.EventProgress payload) throws IOException {
        try {
            Message message = this.progressDefaultInstance.newBuilderForType()
                    .mergeFrom(payload.getData())
                    .build();
            this.progressHandler.handle((TPROG) message);
        } catch (InvalidProtocolBufferException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void onCompleteMessage(Frames.EventComplete payload) throws IOException {
        try {
            if (Frames.EventStatus.OK.equals(payload.getStatus())) {
                Message message = this.progressDefaultInstance.newBuilderForType()
                        .mergeFrom(payload.getData())
                        .build();
                this.completableFuture.complete((TRES) message);
            } else {
                this.completableFuture.completeExceptionally(new RuntimeException(payload.getStatus().toString()));
            }
        } catch (InvalidProtocolBufferException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void onError(Throwable e) {
        this.completableFuture.completeExceptionally(e);
    }
}
