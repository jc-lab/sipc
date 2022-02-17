package kr.jclab.javautils.sipc.event;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.Message;
import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;

public class CalleeRequestContext<T extends GeneratedMessageV3> extends RequestContext {
    private final String streamId;
    private final EventChannel eventChannel;
    private final T request;

    public CalleeRequestContext(EventChannel eventChannel, String streamId, Message request) {
        super(eventChannel, streamId);
        this.eventChannel = eventChannel;
        this.streamId = streamId;
        this.request = (T) request;
    }

    public T getRequest() {
        return this.request;
    }

    public void progress(GeneratedMessageV3 message) {
        try {
            this.eventChannel.sendApplicationData(
                    Frames.EventProgress.newBuilder()
                            .setStreamId(this.streamId)
                            .setData(message.toByteString())
                            .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void complete(GeneratedMessageV3 response) {
        try {
            this.eventChannel.sendApplicationData(
                    Frames.EventComplete.newBuilder()
                            .setStreamId(this.streamId)
                            .setData(response.toByteString())
                            .build()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.eventChannel.done(this);
    }

    @Override
    public void onError(Throwable e) {
        this.eventChannel.done(this);
    }
}
