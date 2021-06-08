package kr.jclab.javautils.sipc.grpc;

import com.google.protobuf.ByteString;
import io.grpc.*;
import io.grpc.internal.SerializingExecutor;
import kr.jclab.sipc.common.proto.Frames;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

// Callee
public class ServerCallImpl<RequestT, ResponseT> extends ServerCall<RequestT, ResponseT> {
    private final AbstractMiddleGrpcChannel middleGrpcChannel;
    private final String streamId;
    private final ServerServiceDefinition serviceDefinition;
    private final ServerMethodDefinition<RequestT, ResponseT> methodDefinition;
    private final ServerCall.Listener<RequestT> listener;
    private final Executor executor;
    private final Executor callExecutor;
    private final Context context;

    private boolean cancelled = false;

    public ServerCallImpl(
            AbstractMiddleGrpcChannel middleGrpcChannel,
            String streamId,
            ServerServiceDefinition serviceDefinition,
            ServerMethodDefinition<RequestT, ResponseT> methodDefinition,
            Metadata headers,
            Executor executor
    ) {
        this.middleGrpcChannel = middleGrpcChannel;
        this.streamId = streamId;
        this.serviceDefinition = serviceDefinition;
        this.methodDefinition = methodDefinition;
        this.listener = methodDefinition.getServerCallHandler()
                .startCall(this, headers);
        this.executor = executor;
        this.callExecutor = new SerializingExecutor(executor);
        this.context = Context.current();
    }

    public void feedReady() {
        this.callExecutor.execute(new Ready());
    }

    public String getStreamId() {
        return this.streamId;
    }

    @Override
    public void request(int numMessages) {

    }

    @Override
    public void sendHeaders(Metadata headers) {
        Frames.GrpcMetadata.Builder headersBuilder = Frames.GrpcMetadata.newBuilder();
        MetadataHelper.serializeTo(headersBuilder, headers);

        try {
            this.middleGrpcChannel.sendPayloadToRemote(
                    Frames.GrpcHeaders.newBuilder()
                            .setStreamId(this.streamId)
                            .setHeaders(headersBuilder.build())
                            .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(ResponseT message) {
        try (InputStream inputStream = this.methodDefinition.getMethodDescriptor().getResponseMarshaller().stream(message)) {
            byte[] encodedMessage = IOUtils.readFully(inputStream, inputStream.available());
            this.middleGrpcChannel.sendPayloadToRemote(
                    Frames.GrpcMessage.newBuilder()
                            .setStreamId(this.streamId)
                            .setMessage(ByteString.copyFrom(encodedMessage))
                            .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close(Status status, Metadata trailers) {
        Frames.GrpcMetadata.Builder trailersBuilder = Frames.GrpcMetadata.newBuilder();
        MetadataHelper.serializeTo(trailersBuilder, trailers);
        try {
            this.middleGrpcChannel.sendPayloadToRemote(
                    Frames.GrpcComplete.newBuilder()
                            .setStreamId(this.streamId)
                            .setStatus(status.getCode().value())
                            .setTrailers(trailersBuilder.build())
                            .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public MethodDescriptor<RequestT, ResponseT> getMethodDescriptor() {
        return this.methodDefinition.getMethodDescriptor();
    }

    public void onRecvMessage(Frames.GrpcMessage payload) {
        this.callExecutor.execute(new RecvMessage(payload));
    }

    public void onRecvHalfClose(Frames.GrpcHalfClose payload) {
        this.callExecutor.execute(new RecvHalfClose(payload));
    }

    public void onRecvComplete(Frames.GrpcComplete payload) {
        if (payload.getStatus() == Status.CANCELLED.getCode().value()) {
            this.cancelled = true;
        }
        this.callExecutor.execute(new RecvComplete(payload));
    }

    class Ready extends ContextRunnable {
        Ready() {
            super(context);
        }
        @Override
        public void runInContext() {
            listener.onReady();
        }
    }

    class RecvMessage extends ContextRunnable {
        private final Frames.GrpcMessage payload;
        RecvMessage(Frames.GrpcMessage payload) {
            super(context);
            this.payload = payload;
        }
        @Override
        public void runInContext() {
            RequestT requestData = methodDefinition.getMethodDescriptor()
                    .getRequestMarshaller().parse(new ByteArrayInputStream(payload.getMessage().toByteArray()));
            listener.onMessage(requestData);
        }
    }

    class RecvHalfClose extends ContextRunnable {
        private final Frames.GrpcHalfClose payload;
        RecvHalfClose(Frames.GrpcHalfClose payload) {
            super(context);
            this.payload = payload;
        }
        @Override
        public void runInContext() {
            listener.onHalfClose();
        }
    }

    class RecvComplete extends ContextRunnable {
        private final Frames.GrpcComplete payload;
        RecvComplete(Frames.GrpcComplete payload) {
            super(context);
            this.payload = payload;
        }
        @Override
        public void runInContext() {
            listener.onComplete();
        }
    }
}
