package kr.jclab.javautils.sipc;

//import io.grpc.BindableService;
//import io.grpc.Channel;
import kr.jclab.javautils.sipc.bson.SipcBsonHelper;
import kr.jclab.javautils.sipc.channel.ChannelHost;
import kr.jclab.javautils.sipc.channel.IpcChannel;
import kr.jclab.javautils.sipc.channel.IpcChannelListener;
import kr.jclab.javautils.sipc.channel.IpcChannelStatus;
import kr.jclab.javautils.sipc.crypto.CryptoException;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyAlgorithmFactory;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;
import kr.jclab.javautils.sipc.event.EventChannel;
//import kr.jclab.javautils.sipc.grpc.MiddleGrpcChannel;
import kr.jclab.javautils.sipc.handler.DoneHandler;
import kr.jclab.javautils.sipc.handler.HandshakeHandler;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

public abstract class SipcHost {
    protected final Executor executor;

    private final CompletableFuture<Void> handshakeFuture = new CompletableFuture<>();
    private HandshakeHandler handshakeHandler;
    private final CompletableFuture<Void> doneFuture = new CompletableFuture<>();
    private DoneHandler doneHandler;

//    private final MiddleGrpcChannel grpcChannel;
    private final EventChannel eventChannel;

    protected final IpcChannel channel;
    private final EphemeralKeyPair keyPair;

    public SipcHost(ChannelHost channelHost, EphemeralKeyAlgorithmFactory keyPairGenerator, Executor executor) throws CryptoException {
        EphemeralKeyPair keyPair = keyPairGenerator.getHostKeyPairGenerator().generate();
        this.keyPair = keyPair;
        this.channel = channelHost.createChannel(this.ipcChannelListener, keyPair);
        this.executor = executor;
//        this.grpcChannel = new MiddleGrpcChannel(executor, this.channel);
        this.eventChannel = new EventChannel(executor, this.channel);
    }

    private final IpcChannelListener ipcChannelListener = new IpcChannelListener() {
        @Override
        public void onChangeChannelStatus(IpcChannelStatus channelStatus) {
            switch (channelStatus) {
                case Established:
                    feedHandshake();
                    break;
            }
        }

        @Override
        public void onError(Throwable e) {
            onError(e);
        }
    };

    public String getConnectInfo() {
        return Base64.getUrlEncoder()
                .encodeToString(
                        SipcBsonHelper.getInstance()
                                .encode(this.channel.getConnectInfo())
                );
    }

//    public void bindService(BindableService bindableService) {
//        this.grpcChannel.bindService(bindableService);
//    }

    public Future<Void> handshake() {
        return this.handshakeFuture;
    }

    public SipcHost onHandshake(HandshakeHandler handler) {
        this.handshakeHandler = handler;
        return this;
    }

    public Future<Void> waitForDone() {
        return this.doneFuture;
    }

    public SipcHost onDone(DoneHandler handler) {
        this.doneHandler = handler;
        return this;
    }

//    public Channel getGrpcChannel() {
//        return this.grpcChannel;
//    }

    public EventChannel getEventChannel() {
        return eventChannel;
    }

    protected void feedHandshake() {
        this.handshakeFuture.complete(null);
        this.executor.execute(() -> {
            if (this.handshakeHandler != null) {
                this.handshakeHandler.handshake();
            }
        });
    }

    protected void feedError(Throwable e) {
        if (!this.handshakeFuture.isDone()) {
            this.handshakeFuture.completeExceptionally(e);
        }
        this.doneFuture.completeExceptionally(e);
        this.executor.execute(() -> {
            if (this.doneHandler != null) {
                this.doneHandler.done(e);
            }
        });
    }

    protected void feedDone() {
        if (!this.handshakeFuture.isDone()) {
            this.handshakeFuture.completeExceptionally(new IOException("The process died early"));
        }
        this.doneFuture.complete(null);
        this.executor.execute(() -> {
            if (this.doneHandler != null) {
                this.doneHandler.done(null);
            }
        });
    }
}
