package kr.jclab.javautils.sipc.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.*;
import kr.jclab.javautils.sipc.ProtoMessageHouse;
import kr.jclab.javautils.sipc.channel.IpcChannel;
import kr.jclab.javautils.sipc.channel.IpcChannelStatus;
import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class AbstractMiddleGrpcChannel extends Channel {
    static final Status WAIT_FOR_READY_ERROR =
            Status.UNAVAILABLE.withDescription(
                    "wait-for-ready RPC is not supported on MiddleGrpcChannel");
    static final Status NOT_READY_ERROR =
            Status.UNAVAILABLE.withDescription(
                    "Not Ready");

    private final Executor executor;
    private final IpcChannel transport;

    /**
     * Key : Service Name
     */
    protected final Map<String, ServerServiceDefinition> serverServiceDefinitions = new HashMap<>();

    /**
     * Key : Method Name
     */
    protected final Map<String, ServerServiceDefinition> serverServiceDefinitionsByMethodName = new HashMap<>();

    private final Map<String, AbstractCallContext> runningCalls = new HashMap<>();

    public AbstractMiddleGrpcChannel(Executor executor, IpcChannel transport) {
        this.executor = executor;
        this.transport = transport;

        transport.registerWrappedData(
                Frames.GrpcRequest.getDefaultInstance(),
                (wrappedData, decodedMessage) -> {
                    ServerServiceDefinition serviceDefinition = serverServiceDefinitionsByMethodName.get(decodedMessage.getMethodName());
                    ServerMethodDefinition<?, ?> methodDefinition = serviceDefinition.getMethod(decodedMessage.getMethodName());

                    Metadata headers = MetadataHelper.parse(decodedMessage.getHeaders());
                    ServerCallImpl<?, ?> serverCall = new ServerCallImpl<>(this, decodedMessage.getStreamId(), serviceDefinition, methodDefinition, headers, executor);
                    this.runningCalls.put(decodedMessage.getStreamId(), new ServerCallContext(serverCall));
                    serverCall.feedReady();
                }
        );
        transport.registerWrappedData(
                Frames.GrpcMessage.getDefaultInstance(),
                (wrappedData, decodedMessage) -> {
                    AbstractCallContext callContext = this.runningCalls.get(decodedMessage.getStreamId());
                    if (callContext == null) {
                        System.err.println("call context is null");
                        return ;
                    }
                    callContext.onRecvMessage(decodedMessage);
                }
        );
        transport.registerWrappedData(
                Frames.GrpcHeaders.getDefaultInstance(),
                (wrappedData, decodedMessage) -> {
                    AbstractCallContext callContext = this.runningCalls.get(decodedMessage.getStreamId());
                    if (callContext == null) {
                        System.err.println("call context is null");
                        return ;
                    }
                    callContext.onRecvHeaders(decodedMessage);
                }
        );
        transport.registerWrappedData(
                Frames.GrpcHalfClose.getDefaultInstance(),
                (wrappedData, decodedMessage) -> {
                    AbstractCallContext callContext = this.runningCalls.get(decodedMessage.getStreamId());
                    if (callContext == null) {
                        System.err.println("call context is null");
                        return ;
                    }
                    callContext.onRecvHalfClose(decodedMessage);
                }
        );
        transport.registerWrappedData(
                Frames.GrpcComplete.getDefaultInstance(),
                (wrappedData, decodedMessage) -> {
                    AbstractCallContext callContext = this.runningCalls.get(decodedMessage.getStreamId());
                    if (callContext == null) {
                        System.err.println("call context is null");
                        return ;
                    }
                    callContext.onRecvComplete(decodedMessage);
                }
        );
    }

    public void sendPayloadToRemote(GeneratedMessageV3 messageV3) throws IOException {
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

    public void bindService(BindableService bindableService) {
        ServerServiceDefinition definition = bindableService.bindService();
        this.serverServiceDefinitions.put(definition.getServiceDescriptor().getName(), definition);
        for (ServerMethodDefinition<?, ?> method : definition.getMethods()) {
            this.serverServiceDefinitionsByMethodName.put(method.getMethodDescriptor().getFullMethodName(), definition);
        }
    }

    public void clientCallDone(ClientCallImpl impl) {
        this.runningCalls.remove(impl.getStreamId());
    }

    public void serverCallDone(ServerCallImpl impl) {
        this.runningCalls.remove(impl.getStreamId());
    }

    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT> newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
        final Executor effectiveExecutor =
                callOptions.getExecutor() == null ? this.executor : callOptions.getExecutor();
        final IpcChannelStatus channelStatus = transport.getChannelStatus();

        if (!IpcChannelStatus.Established.equals(channelStatus)) {
            if (callOptions.isWaitForReady()) {
                // Not supported
                return new PreErrorClientCall<RequestT, ResponseT>(effectiveExecutor, WAIT_FOR_READY_ERROR);
            } else {
                return new PreErrorClientCall<RequestT, ResponseT>(effectiveExecutor, NOT_READY_ERROR);
            }
        }

        ClientCallImpl<RequestT, ResponseT> impl = new ClientCallImpl<RequestT, ResponseT>(
                this,
                methodDescriptor,
                callOptions,
                effectiveExecutor
        );
        this.runningCalls.put(impl.getStreamId(), new ClientCallContext(impl));
        return impl;
    }

    @Override
    public String authority() {
        return null;
    }

    private static class PreErrorClientCall<RequestT, ResponseT> extends ClientCall<RequestT, ResponseT> {
        private final Executor executor;
        private final Status status;

        public PreErrorClientCall(Executor executor, Status status) {
            this.executor = executor;
            this.status = status;
        }

        @Override
        public void start(final ClientCall.Listener<ResponseT> listener, Metadata headers) {
            this.executor.execute(new Runnable() {
                @Override
                public void run() {
                    listener.onClose(NOT_READY_ERROR, new Metadata());
                }
            });
        }

        @Override
        public void request(int numMessages) {
        }

        @Override
        public void cancel(String message, Throwable cause) {
        }

        @Override
        public void halfClose() {
        }

        @Override
        public void sendMessage(RequestT message) {
        }
    }
}
