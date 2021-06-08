package kr.jclab.javautils.sipc.event;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import kr.jclab.javautils.sipc.ProtoMessageHouse;
import kr.jclab.javautils.sipc.channel.IpcChannel;
import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public class EventChannel {
    private final Executor executor;
    private final IpcChannel transport;

    private final Map<String, OnRequestMethod<? extends GeneratedMessageV3>> requestMethods = new HashMap<>();
    private final Map<String, RequestContext> runningCalls = new HashMap<>();

    @FunctionalInterface
    public interface ProtoHandler<T extends GeneratedMessageV3> {
        void handle(T message);
    }

    @FunctionalInterface
    public interface RequestHandler<T extends GeneratedMessageV3> {
        void handle(CalleeRequestContext<T> requestContext);
    }

    public EventChannel(Executor executor, IpcChannel transport) {
        this.executor = executor;
        this.transport = transport;

        transport.addCleanupHandler(() -> {
            for (RequestContext requestContext : this.runningCalls.values()) {
                requestContext.onError(new IOException("Connection is closed"));
            }
        });
        transport.registerWrappedData(
                Frames.EventRequest.getDefaultInstance(),
                (wrappedData, payload) -> {
                    final OnRequestMethod method = requestMethods.get(payload.getMethodName());
                    if (method == null) {
                        throw new IOException("NOT EXISTS METHOD: " + payload.getMethodName());
                    }

                    Message requestMessage = method.getRequestDefaultInstance()
                            .newBuilderForType()
                            .mergeFrom(payload.getData())
                            .build();

                    final CalleeRequestContext<?> requestContext = new CalleeRequestContext<>(this, payload.getStreamId(), requestMessage);
                    this.runningCalls.put(payload.getStreamId(), requestContext);
                    this.executor.execute(new Runnable() {
                        @Override
                        public void run() {
                            method.handler.handle(requestContext);
                        }
                    });
                }
        );
        transport.registerWrappedData(
                Frames.EventProgress.getDefaultInstance(),
                (wrappedData, payload) -> {
                    RequestContext requestContext = this.runningCalls.get(payload.getStreamId());
                    if (requestContext == null) {
                        throw new IOException("Not exists request : " + payload.getStreamId());
                    }
                    requestContext.onProgressMessage(payload);
                }
        );
        transport.registerWrappedData(
                Frames.EventComplete.getDefaultInstance(),
                (wrappedData, payload) -> {
                    RequestContext requestContext = this.runningCalls.get(payload.getStreamId());
                    if (requestContext == null) {
                        throw new IOException("Not exists request : " + payload.getStreamId());
                    }
                    requestContext.onCompleteMessage(payload);
                    this.done(requestContext);
                }
        );
    }

    public <TREQ extends GeneratedMessageV3> void onRequest(
            String requestName,
            TREQ requestDefaultInstance,
            RequestHandler<TREQ> handler
    ) {
        this.requestMethods.put(requestName, new OnRequestMethod<>(requestName, requestDefaultInstance, handler));
    }

    public <TRES extends GeneratedMessageV3> Future<TRES> request(
            String requestName,
            GeneratedMessageV3 request,
            TRES responseDefaultInstance
    ) throws IOException {
        return this.request(requestName, request, responseDefaultInstance, null, null);
    }

    public <TRES extends GeneratedMessageV3, TPROG extends GeneratedMessageV3> Future<TRES> request(
            String requestName,
            GeneratedMessageV3 request,
            TRES responseDefaultInstance,
            TPROG progressDefaultInstance,
            ProtoHandler<TPROG> progressHandler
    ) throws IOException {
        CallerRequestContext<TRES, TPROG> requestContext = new CallerRequestContext<TRES, TPROG>(this, UUID.randomUUID().toString(), responseDefaultInstance, progressDefaultInstance, progressHandler);
        this.runningCalls.put(requestContext.getStreamId(), requestContext);
        this.sendApplicationData(
                Frames.EventRequest.newBuilder()
                        .setMethodName(requestName)
                        .setStreamId(requestContext.getStreamId())
                        .setData(request.toByteString())
                        .build()
        );
        return requestContext.getFuture();
    }

    public void sendApplicationData(GeneratedMessageV3 messageV3) throws IOException {
        this.transport.sendWrappedData(
                Frames.WrappedData.newBuilder()
                        .setVersion(1)
                        .setMessage(Any.newBuilder()
                                .setTypeUrl(ProtoMessageHouse.getTypeUrl(messageV3))
                                .setValue(messageV3.toByteString())
                                .build())
                        .build()
        );
    }

    public void done(RequestContext requestContext) {
        this.runningCalls.remove(requestContext.getStreamId());
    }

    private static class OnRequestMethod<TREQ extends GeneratedMessageV3> {
        private final String requestName;
        private final TREQ requestDefaultInstance;
        private final RequestHandler<TREQ> handler;

        public OnRequestMethod(String requestName, TREQ requestDefaultInstance, RequestHandler<TREQ> handler) {
            this.requestName = requestName;
            this.requestDefaultInstance = requestDefaultInstance;
            this.handler = handler;
        }

        public String getRequestName() {
            return requestName;
        }

        public TREQ getRequestDefaultInstance() {
            return requestDefaultInstance;
        }

        public RequestHandler<TREQ> getHandler() {
            return handler;
        }
    }
}
