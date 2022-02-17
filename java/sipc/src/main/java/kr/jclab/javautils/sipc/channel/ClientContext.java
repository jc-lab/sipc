package kr.jclab.javautils.sipc.channel;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.GeneratedMessageV3;
import kr.jclab.sipc.common.proto.Frames;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per Connection
 */
public abstract class ClientContext {
    protected final ByteBuffer receivingBuffer;
    private final FrameDecoder frameDecoder;
    private final AtomicReference<FrameConverter.FrameHandlers> frameHandlers;

    public ClientContext() {
        this.receivingBuffer = FrameDecoder.createBuffer(16777216);
        ((Buffer)this.receivingBuffer).clear();

        this.frameDecoder = new FrameDecoder(this.receivingBuffer);
        this.frameHandlers = new AtomicReference<>(this.preRoutedFrameHandlers);
    }

    public void cleanup() {
        this.frameHandlers.get().onCleanup();
    }

    // ============================== RECEIVE ==============================

    /**
     * Call after receiveBuffer has been updated.
     *
     * @throws IOException
     */
    protected void recvAfterReadRaw() throws IOException {
        this.frameDecoder.recvAfterReadRaw(this.frameHandlers.get());
    }

    protected void recvSetFrameHandlers(FrameConverter.FrameHandlers frameHandlers) {
        this.frameHandlers.set(frameHandlers);
    }

    protected abstract void recvDecideRoute(String channelId);

    private final FrameConverter.FrameHandlers preRoutedFrameHandlers = new FrameConverter.FrameHandlers() {
        @Override
        public void onAlertFrame(Frames.AlertFrame frame) throws IOException {
            throw new IOException("Illegal State");
        }

        @Override
        public void onServerHello(Frames.ServerHelloFrame frame) throws IOException {
            throw new IOException("Illegal State");
        }

        @Override
        public void onClientHello(Frames.ClientHelloFrame frame) throws IOException {
            recvDecideRoute(frame.getChannelId());
            FrameConverter.FrameHandlers newFrameHandlers = frameHandlers.get();
            if (newFrameHandlers != preRoutedFrameHandlers) {
                newFrameHandlers.onClientHello(frame);
                return ;
            }
            throw new IOException("Illegal State");
        }

        @Override
        public void onWrappedData(Frames.EncryptedWrappedData frame) throws IOException {
            throw new IOException("Illegal State");
        }

        @Override
        public void onCleanup() {

        }
    };

    // ============================== SEND ==============================

    protected abstract void sendRaw(byte[] data) throws IOException;

    @VisibleForTesting
    public void sendFrame(GeneratedMessageV3 message) throws IOException {
        this.sendRaw(
                FrameConverter.getInstance().encodeFrame(message)
        );
    }
}
