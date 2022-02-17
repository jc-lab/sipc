package kr.jclab.javautils.sipc.channel;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FrameDecoder {
    public static ByteBuffer createBuffer(int size) {
        return ByteBuffer.allocate(size)
                .order(ByteOrder.LITTLE_ENDIAN);
    }

    public final ByteBuffer receivingBuffer;

    public FrameDecoder(ByteBuffer receivingBuffer) {
        this.receivingBuffer = receivingBuffer;
    }

    /**
     * Call after receiveBuffer has been updated.
     *
     * @throws IOException
     */
    public void recvAfterReadRaw(FrameConverter.FrameHandlers frameHandlers) throws IOException {
        boolean hasNextPacket;
        do {
            hasNextPacket = false;
            int availableLength = this.receivingBuffer.position();
            if (availableLength >= 4) {
                int frameSize = FrameConverter.toInt24(this.receivingBuffer.getInt(0));
                if (availableLength >= frameSize) {
                    ((Buffer)this.receivingBuffer).flip();
                    processFrame(frameSize, frameHandlers);
                    if (this.receivingBuffer.remaining() > 0) {
                        //TODO: More efficiently
                        byte[] temp = new byte[this.receivingBuffer.remaining()];
                        this.receivingBuffer.get(temp);
                        ((Buffer)this.receivingBuffer).clear();
                        this.receivingBuffer.put(temp);
                        hasNextPacket = true;
                    } else {
                        ((Buffer)this.receivingBuffer).clear();
                    }
                }
            }
        } while(hasNextPacket);
    }

    public void processFrame(int frameSize, FrameConverter.FrameHandlers frameHandlers) throws IOException {
        this.receivingBuffer.get();
        this.receivingBuffer.get();
        this.receivingBuffer.get();
        byte frameType = this.receivingBuffer.get();
        byte[] payload = new byte[frameSize - 4];
        this.receivingBuffer.get(payload);

        try {
            FrameConverter.getInstance().parse(frameHandlers, frameType, payload);
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }
}
