package fake;

import com.google.protobuf.GeneratedMessageV3;
import kr.jclab.javautils.sipc.ProtoMessageHouse;
import kr.jclab.javautils.sipc.bson.entity.ConnectInfo;
import kr.jclab.javautils.sipc.channel.*;
import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class PeeredIpcChannel implements IpcChannel {
    private final String label;
    private final String channelId = UUID.randomUUID().toString();
    private final Executor executor;
    private PeeredIpcChannel remote = null;

    private final List<CleanupHandler> cleanupHandlers = new ArrayList<>();

    public PeeredIpcChannel(String label, Executor executor) {
        this.label = label;
        this.executor = executor;
    }

    public String getLabel() {
        return label;
    }

    public void setRemote(PeeredIpcChannel remote) {
        this.remote = remote;
    }

    @Override
    public void onAlertFrame(Frames.AlertFrame frame) throws IOException {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public void onServerHello(Frames.ServerHelloFrame frame) throws IOException {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public void onClientHello(Frames.ClientHelloFrame frame) throws IOException {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public void onWrappedData(Frames.EncryptedWrappedData frame) throws IOException {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public void onCleanup() {
        boolean hasException = false;
        RuntimeException exception = new RuntimeException("exceptions");
        for (CleanupHandler handler : this.cleanupHandlers) {
            try {
                handler.cleanup();
            } catch (Throwable e) {
                hasException = true;
                exception.addSuppressed(e);
            }
        }
        if (hasException) {
            throw exception;
        }
    }

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final LinkedBlockingQueue<Frames.WrappedData> inputQueue = new LinkedBlockingQueue<>();
    public void feedWrappedData(Frames.WrappedData wrappedData) throws IOException {
        try {
            inputQueue.put(wrappedData);
            this.ioExecutor.execute(() -> {
                try {
                    Frames.WrappedData item = inputQueue.poll();
                    WrappedDataReceiverHolder<GeneratedMessageV3> receiver = (WrappedDataReceiverHolder<GeneratedMessageV3>) this.wrappedDataReceivers.get(item.getMessage().getTypeUrl());
                    if (receiver == null) {
                        System.err.println("receiver is null : " + item.getMessage().getTypeUrl());
                        return ;
                    }
                    GeneratedMessageV3 decodedMessage = (GeneratedMessageV3)receiver.messageDefaultInstance.newBuilderForType()
                            .mergeFrom(item.getMessage().getValue())
                            .build();
                    System.out.println("PEER[" + this.label + "]: " + decodedMessage.getDescriptorForType().getFullName() + "\n" + decodedMessage);
                    receiver.receiver.onMessage(item, decodedMessage);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getChannelId() {
        return this.channelId;
    }

    @Override
    public ConnectInfo getConnectInfo() {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public void attachClientContext(ClientContext clientContext) {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public IpcChannelStatus getChannelStatus() {
        return remote == null ? IpcChannelStatus.Connecting : IpcChannelStatus.Established;
    }

    @Override
    public void sendFrame(GeneratedMessageV3 frame) throws IOException {
        throw new IllegalStateException("Not Implemented");
    }

    @Override
    public void sendWrappedData(Frames.WrappedData wrappedData) throws IOException {
        remote.feedWrappedData(wrappedData);
    }

    private final Map<String, WrappedDataReceiverHolder<?>> wrappedDataReceivers = new HashMap<>();

    @Override
    public <T extends GeneratedMessageV3> void registerWrappedData(T messageDefaultInstance, WrappedDataReceiver<T> receiver) {
        String typeUrl = ProtoMessageHouse.getTypeUrl(messageDefaultInstance);
        this.wrappedDataReceivers.compute(typeUrl, (k, old) -> {
            if (old != null) {
                throw new IllegalArgumentException("Duplicated type url");
            }
            return new WrappedDataReceiverHolder<T>(messageDefaultInstance, receiver);
        });
    }

    @Override
    public void addCleanupHandler(CleanupHandler cleanupHandler) {
        this.cleanupHandlers.add(cleanupHandler);
    }

    private static class WrappedDataReceiverHolder<T extends GeneratedMessageV3> {
        public final GeneratedMessageV3 messageDefaultInstance;
        public final WrappedDataReceiver<T> receiver;

        public WrappedDataReceiverHolder(GeneratedMessageV3 messageDefaultInstance, WrappedDataReceiver<T> receiver) {
            this.messageDefaultInstance = messageDefaultInstance;
            this.receiver = receiver;
        }
    }
}
