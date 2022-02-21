package kr.jclab.javautils.sipc.event;

import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class CallerRequestContext extends RequestContext {
    private final Consumer<byte[]> progressHandler;
    private final CompletableFuture<byte[]> completableFuture = new CompletableFuture<>();

    public CallerRequestContext(
            EventChannel eventChannel,
            String streamId,
            Consumer<byte[]> progressHandler
            ) {
        super(eventChannel, streamId);
        this.progressHandler = progressHandler;
    }

    public CompletableFuture<byte[]> getFuture() {
        return this.completableFuture;
    }

    @Override
    public void onProgressMessage(Frames.EventProgress payload) throws IOException {
        this.progressHandler.accept(payload.getData().toByteArray());
    }

    @Override
    public void onCompleteMessage(Frames.EventComplete payload) throws IOException {
        if (Frames.EventStatus.OK.equals(payload.getStatus())) {
            this.completableFuture.complete(payload.getData().toByteArray());
        } else {
            this.completableFuture.completeExceptionally(new RuntimeException(payload.getStatus().toString()));
        }
    }

    @Override
    public void onError(Throwable e) {
        this.completableFuture.completeExceptionally(e);
    }
}
