package kr.jclab.javautils.sipc.channel.tcp;

import kr.jclab.javautils.sipc.bson.entity.ConnectInfo;
import kr.jclab.javautils.sipc.bson.entity.TcpConnectInfo;
import kr.jclab.javautils.sipc.channel.AbstractIpcChannel;
import kr.jclab.javautils.sipc.channel.IpcChannelListener;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;

import java.util.UUID;

public class TcpIpcChannel extends AbstractIpcChannel {
    private final String channelType;
    private final String address;
    private final int port;

    public TcpIpcChannel(IpcChannelListener ipcChannelListener, EphemeralKeyPair serverKey, String channelType, String address, int port) {
        super(UUID.randomUUID().toString(), ipcChannelListener, serverKey);
        this.channelType = channelType;
        this.address = address;
        this.port = port;
    }

    @Override
    public ConnectInfo getConnectInfo() {
        return TcpConnectInfo.builder()
                .channelType(this.channelType)
                .channelId(this.channelId)
                .algo(this.myKey.getAlgorithm())
                .publicKey(this.myKey.getPublicKey())
                .address(this.address)
                .port(this.port)
                .build();
    }
}
