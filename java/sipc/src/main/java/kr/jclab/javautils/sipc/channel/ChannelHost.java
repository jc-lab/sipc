package kr.jclab.javautils.sipc.channel;

import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;

import java.io.Closeable;

public interface ChannelHost extends Closeable {
    String getType();
    IpcChannel createChannel(IpcChannelListener ipcChannelListener, EphemeralKeyPair serverKey);
}
