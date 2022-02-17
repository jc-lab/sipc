package kr.jclab.javautils.sipc.event;

import kr.jclab.sipc.common.proto.Frames;

import java.io.IOException;

public class RequestContext {
    private final EventChannel eventChannel;
    private final String streamId;

    public RequestContext(EventChannel eventChannel, String streamId) {
        this.eventChannel = eventChannel;
        this.streamId = streamId;
    }

    public String getStreamId() {
        return streamId;
    }

    public void onProgressMessage(Frames.EventProgress payload) throws IOException {}

    public void onCompleteMessage(Frames.EventComplete payload) throws IOException {}

    public void onError(Throwable e) {}
}
