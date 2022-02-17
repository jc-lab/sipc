package kr.jclab.javautils.sipc.channel.stdio;

import kr.jclab.javautils.sipc.bson.entity.ConnectInfo;
import kr.jclab.javautils.sipc.channel.AbstractIpcChannel;
import kr.jclab.javautils.sipc.channel.IpcChannelListener;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;

import java.util.UUID;

public class StdioIpcChannel extends AbstractIpcChannel {
    public StdioIpcChannel(IpcChannelListener ipcChannelListener, EphemeralKeyPair serverKey) {
        super(false, UUID.randomUUID().toString(), ipcChannelListener, serverKey);
    }

    @Override
    public ConnectInfo getConnectInfo() {
        return ConnectInfo.basicBuilder()
                .channelType(StdioChannelHost.CHANNEL_TYPE)
                .channelId(this.channelId)
                .algo(this.myKey.getAlgorithm())
                .publicKey(this.myKey.getPublicKey())
                .build();
    }
}
