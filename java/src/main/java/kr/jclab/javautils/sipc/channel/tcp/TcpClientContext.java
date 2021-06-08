package kr.jclab.javautils.sipc.channel.tcp;

import kr.jclab.javautils.sipc.channel.ClientContext;
import kr.jclab.javautils.sipc.channel.IpcChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class TcpClientContext extends ClientContext {
    private final TcpChannelHost tcpChannelHost;
    private final SocketChannel clientChannel;

    public TcpClientContext(TcpChannelHost tcpChannelHost, SocketChannel clientChannel) {
        super();
        this.tcpChannelHost = tcpChannelHost;
        this.clientChannel = clientChannel;
    }

    public boolean doRead() throws IOException {
        int readLength = this.clientChannel.read(this.receivingBuffer);
        if (readLength <= 0) {
            return false;
        }
        this.recvAfterReadRaw();
        return true;
    }

    @Override
    protected void sendRaw(byte[] data) throws IOException {
        this.clientChannel.write(ByteBuffer.wrap(data));
    }

    @Override
    protected void recvDecideRoute(String channelId) {
        IpcChannel ipcChannel = this.tcpChannelHost.findIpcChannel(channelId);
        if (ipcChannel != null) {
            ipcChannel.attachClientContext(this);
            this.recvSetFrameHandlers(ipcChannel);
        }
    }
}
