package kr.jclab.javautils.sipc.channel.stdio;

import kr.jclab.javautils.sipc.channel.ChannelHost;
import kr.jclab.javautils.sipc.DefaultChannelType;
import kr.jclab.javautils.sipc.channel.IpcChannel;
import kr.jclab.javautils.sipc.channel.IpcChannelListener;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;

import java.io.IOException;

public class StdioChannelHost implements ChannelHost {
    public static final String CHANNEL_TYPE = DefaultChannelType.Stdio.value();


    private static class LazyHolder {
        private static final StdioChannelHost INSTANCE = new StdioChannelHost();
    }

    public static StdioChannelHost getInstance() {
        return LazyHolder.INSTANCE;
    }

    @Override
    public String getType() {
        return CHANNEL_TYPE;
    }

    @Override
    public IpcChannel createChannel(IpcChannelListener ipcChannelListener, EphemeralKeyPair serverKey) {
        return new StdioIpcChannel(ipcChannelListener, serverKey);
    }

    @Override
    public void close() throws IOException {

    }
}
