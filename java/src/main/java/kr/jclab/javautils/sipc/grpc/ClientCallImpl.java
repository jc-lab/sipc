package kr.jclab.javautils.sipc.grpc;

import com.google.protobuf.GeneratedMessageV3;
import io.grpc.*;
import io.grpc.internal.SerializingExecutor;
import kr.jclab.sipc.common.proto.Frames;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executor;

// Caller
public class ClientCallImpl<RequestT, ResponseT> extends ClientCall<RequestT, ResponseT> {
    private final String streamId = UUID.randomUUID().toString();
    private final AbstractMiddleGrpcChannel middleGrpcChannel;
    private final MethodDescriptor<RequestT, ResponseT> methodDescriptor;
    private final CallOptions callOptions;
    private final Executor executor;
    private final Executor callExecutor;
    private final Context context;

    private Listener<ResponseT> responseListener = null;

    public ClientCallImpl(AbstractMiddleGrpcChannel middleGrpcChannel, MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions, Executor executor) {
        this.middleGrpcChannel = middleGrpcChannel;
        this.methodDescriptor = methodDescriptor;
        this.callOptions = callOptions;
        this.executor = executor;
        this.callExecutor = new SerializingExecutor(executor);
        this.context = Context.current();
    }

    public String getStreamId() {
        return streamId;
    }

    @Override
    public void start(Listener<ResponseT> responseListener, Metadata headers) {
        this.responseListener = responseListener;

        try {
            Frames.GrpcMetadata.Builder headersMessageBuilder = Frames.GrpcMetadata.newBuilder();
            MetadataHelper.serializeTo(headersMessageBuilder, headers);

            this.middleGrpcChannel.sendPayloadToRemote(
                    Frames.GrpcRequest.newBuilder()
                            .setStreamId(this.streamId)
                            .setMethodName(this.methodDescriptor.getFullMethodName())
                            .setHeaders(headersMessageBuilder.build())
                    .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void request(int numMessages) {
        System.out.println("ClientCallImpl::request : " + numMessages);
    }

    @Override
    public void cancel(@Nullable String message, @Nullable Throwable cause) {
        System.out.println("ClientCallImpl::cancel : " + message);
        if (cause != null) {
            cause.printStackTrace();
        }

        this.middleGrpcChannel.clientCallDone(this);

        try {
            this.middleGrpcChannel.sendPayloadToRemote(
                    Frames.GrpcComplete.newBuilder()
                            .setStreamId(this.streamId)
                            .setStatus(Status.CANCELLED.getCode().value())
                            .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void halfClose() {
        Frames.GrpcHalfClose grpcMessage = Frames.GrpcHalfClose.newBuilder()
                .setStreamId(this.streamId)
                .build();
        try {
            this.middleGrpcChannel.sendPayloadToRemote(grpcMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendMessage(RequestT message) {
        Frames.GrpcMessage grpcMessage = Frames.GrpcMessage.newBuilder()
                .setStreamId(this.streamId)
                .setMessage(((GeneratedMessageV3)message).toByteString())
                .build();
        try {
            this.middleGrpcChannel.sendPayloadToRemote(grpcMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onRecvMessage(Frames.GrpcMessage payload) {
        this.callExecutor.execute(new RecvMessage(payload));
    }

    public void onRecvHeaders(Metadata headers) {
        if (this.responseListener == null) {
            throw new IllegalStateException("responseListener is null");
        }
        this.callExecutor.execute(new RecvHeaders(headers));
    }

    public void onRecvComplete(Frames.GrpcComplete payload) {
        this.callExecutor.execute(new RecvComplete(payload));
    }

    private class RecvHeaders extends ContextRunnable {
        private final Metadata headers;
        RecvHeaders(Metadata headers) {
            super(context);
            this.headers = headers;
        }

        @Override
        public void runInContext() {
            responseListener.onHeaders(headers);
        }
    }

    private class RecvMessage extends ContextRunnable {
        private final Frames.GrpcMessage payload;
        RecvMessage(Frames.GrpcMessage payload) {
            super(context);
            this.payload = payload;
        }

        @Override
        public void runInContext() {
            ResponseT responseData = methodDescriptor
                    .getResponseMarshaller().parse(new ByteArrayInputStream(payload.getMessage().toByteArray()));
            responseListener.onMessage(responseData);
        }
    }

    private class RecvComplete extends ContextRunnable {
        private final Frames.GrpcComplete payload;
        RecvComplete(Frames.GrpcComplete payload) {
            super(context);
            this.payload = payload;
        }

        @Override
        public void runInContext() {
            Metadata trailers = MetadataHelper.parse(payload.getTrailers());
            responseListener.onClose(Status.fromCodeValue(payload.getStatus()), trailers);
        }
    }
}
