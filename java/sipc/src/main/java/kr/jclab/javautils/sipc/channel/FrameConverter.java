package kr.jclab.javautils.sipc.channel;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import kr.jclab.sipc.common.proto.Frames;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FrameConverter {
    public static class LazyHolder {
        public static FrameConverter INSTANCE = new FrameConverter();
    }

    public static FrameConverter getInstance() {
        return LazyHolder.INSTANCE;
    }

    @FunctionalInterface
    public interface FrameHandler<T extends GeneratedMessageV3> {
        void handle(FrameHandlers frameHandlers, byte frameType, T framePayload) throws IOException;
    }

    public interface FrameHandlers {
        // Remote To Local
        void onAlertFrame(Frames.AlertFrame frame) throws IOException;
        void onServerHello(Frames.ServerHelloFrame frame) throws IOException;
        void onClientHello(Frames.ClientHelloFrame frame) throws IOException;
        void onWrappedData(Frames.EncryptedWrappedData frame) throws IOException;
        void onCleanup();
    }

    private static class FrameHandlerHolder<T extends GeneratedMessageV3> {
        public final byte type;
        public final T defaultInstance;
        public final FrameHandler<? extends GeneratedMessageV3> handler;

        public FrameHandlerHolder(byte type, T defaultInstance, FrameHandler<T> handler) {
            this.type = type;
            this.defaultInstance = defaultInstance;
            this.handler = handler;
        }
    }

    private final Map<Byte, FrameHandlerHolder<?>> handlers;
    private final Map<Class<? extends GeneratedMessageV3>, FrameHandlerHolder<?>> frameTypes;
    private final Method handleMethod;

    public static void add(
            HashMap<Byte, FrameHandlerHolder<?>> handlers,
            HashMap<Class<? extends GeneratedMessageV3>, FrameHandlerHolder<?>> frameTypes,
            FrameHandlerHolder<?> holder
    ) {
        handlers.put(holder.type, holder);
        frameTypes.put(holder.defaultInstance.getClass(), holder);
    }

    public FrameConverter() {
        HashMap<Byte, FrameHandlerHolder<?>> handlers = new HashMap<>();
        HashMap<Class<? extends GeneratedMessageV3>, FrameHandlerHolder<?>> frameTypes = new HashMap<>();
        try {
            this.handleMethod = FrameHandler.class.getMethod("handle", FrameHandlers.class, byte.class, GeneratedMessageV3.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        add(handlers, frameTypes, new FrameHandlerHolder<>((byte)0xf1, Frames.AlertFrame.getDefaultInstance(), (frameHandlers, frameType, framePayload) -> {
            frameHandlers.onAlertFrame(framePayload);
        }));
        add(handlers, frameTypes, new FrameHandlerHolder<>((byte)0x11, Frames.ClientHelloFrame.getDefaultInstance(), (frameHandlers, frameType, framePayload) -> {
            frameHandlers.onClientHello(framePayload);
        }));
        add(handlers, frameTypes, new FrameHandlerHolder<>((byte)0x12, Frames.ServerHelloFrame.getDefaultInstance(), (frameHandlers, frameType, framePayload) -> {
            frameHandlers.onServerHello(framePayload);
        }));
        add(handlers, frameTypes, new FrameHandlerHolder<>((byte)0x1a, Frames.EncryptedWrappedData.getDefaultInstance(), (frameHandlers, frameType, framePayload) -> {
            frameHandlers.onWrappedData(framePayload);
        }));
        this.handlers = Collections.unmodifiableMap(handlers);
        this.frameTypes = Collections.unmodifiableMap(frameTypes);
    }

    public boolean parse(FrameHandlers frameHandlers, byte frameType, byte[] framePayload) throws InvalidProtocolBufferException {
        FrameHandlerHolder<? extends GeneratedMessageV3> holder = this.handlers.get(frameType);
        if (holder == null)
            return false;
        GeneratedMessageV3 message = (GeneratedMessageV3) holder.defaultInstance.newBuilderForType()
                .mergeFrom(framePayload)
                .build();
        try {
            this.handleMethod.invoke(holder.handler, frameHandlers, frameType, message);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public <T extends GeneratedMessageV3> Byte getFrameType(T message) {
        FrameHandlerHolder<?> holder = this.frameTypes.get(message.getClass());
        if (holder == null) {
            return null;
        }
        return holder.type;
    }

    public byte[] encodeFrame(GeneratedMessageV3 message) throws IOException {
        byte frameType = FrameConverter.getInstance().getFrameType(message);

        int serializedSize = message.getSerializedSize();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(4 + serializedSize);
        ByteBuffer buffer = ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN);
        FrameConverter.writeInt24(buffer, 4 + serializedSize);
        buffer.put(frameType);
        ((Buffer)buffer).flip();
        bos.write(buffer.array(), buffer.arrayOffset(), buffer.remaining());
        message.writeTo(bos);

        return bos.toByteArray();
    }

    public static void writeInt24(ByteBuffer buffer, int v) {
        buffer.put((byte)((v) & 0xff));
        buffer.put((byte)((v >> 8) & 0xff));
        buffer.put((byte)((v >> 16) & 0xff));
    }

    public static int toInt24(int v) {
        return v & 0xffffff;
    }
}
