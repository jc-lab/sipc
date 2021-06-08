package kr.jclab.javautils.sipc.channel;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.GeneratedMessageV3;
import kr.jclab.sipc.common.proto.Frames;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per Connection
 */
public abstract class ClientContext {
    protected final ByteBuffer receivingBuffer = ByteBuffer.allocate(16777216)
            .order(ByteOrder.LITTLE_ENDIAN);

    private final AtomicReference<FrameConverter.FrameHandlers> frameHandlers;

    public ClientContext() {
        this.receivingBuffer.clear();
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
        boolean hasNextPacket;
        do {
            hasNextPacket = false;
            int availableLength = this.receivingBuffer.position();
            if (availableLength >= 4) {
                int frameSize = FrameConverter.toInt24(this.receivingBuffer.getInt(0));
                if (availableLength >= frameSize) {
                    this.receivingBuffer.flip();
                    processFrame(this.receivingBuffer, frameSize);
                    if (this.receivingBuffer.remaining() > 0) {
                        //TODO: More efficiently
                        byte[] temp = new byte[this.receivingBuffer.remaining()];
                        this.receivingBuffer.get(temp);
                        this.receivingBuffer.clear();
                        this.receivingBuffer.put(temp);
                        hasNextPacket = true;
                    } else {
                        this.receivingBuffer.clear();
                    }
                }
            }
        } while(hasNextPacket);
    }

    private void processFrame(ByteBuffer buffer, int frameSize) throws IOException {
        this.receivingBuffer.get();
        this.receivingBuffer.get();
        this.receivingBuffer.get();
        byte frameType = this.receivingBuffer.get();
        byte[] payload = new byte[frameSize - 4];
        buffer.get(payload);

        try {
            FrameConverter.getInstance().parse(this.frameHandlers.get(), frameType, payload);
        } catch (Throwable e) {
            throw new IOException(e);
        }
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
        byte frameType = FrameConverter.getInstance().getFrameType(message);

        int serializedSize = message.getSerializedSize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4 + serializedSize);
        ByteBuffer buffer = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN);
        FrameConverter.writeInt24(buffer, 4 + serializedSize);
        buffer.put(frameType);
        buffer.flip();
        bos.write(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        message.writeTo(bos);

        this.sendRaw(bos.toByteArray());
    }
}
