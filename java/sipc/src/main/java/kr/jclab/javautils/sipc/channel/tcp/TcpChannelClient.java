package kr.jclab.javautils.sipc.channel.tcp;

import com.google.protobuf.GeneratedMessageV3;
import kr.jclab.javautils.sipc.bson.entity.ConnectInfo;
import kr.jclab.javautils.sipc.bson.entity.TcpConnectInfo;
import kr.jclab.javautils.sipc.channel.*;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

@Slf4j
public class TcpChannelClient implements Closeable {
    private static final int COMMAND_CLOSE = 1;

    private final TcpConnectInfo connectInfo;
    private final SocketChannel socketChannel;
    private final AbstractIpcChannel ipcChannel;
    private final Selector selector;
    private final LinkedBlockingDeque<Integer> commandQueue = new LinkedBlockingDeque<>();

    public TcpChannelClient(
            TcpConnectInfo connectInfo,
            EphemeralKeyPair myKey,
            IpcChannelListener ipcChannelListener
    ) throws IOException {
        Selector selector = null;
        SocketChannel socketChannel = null;

        this.connectInfo = connectInfo;
        this.ipcChannel = new AbstractIpcChannel(false, this.connectInfo.getChannelId(), ipcChannelListener, myKey) {
            @Override
            public ConnectInfo getConnectInfo() {
                return null;
            }

            @Override
            public void sendFrame(GeneratedMessageV3 frame) throws IOException {
                TcpChannelClient.this.socketChannel.write(
                        ByteBuffer.wrap(FrameConverter.getInstance().encodeFrame(frame))
                );
            }
        };
        this.ipcChannel.setServerPublicKey(this.connectInfo.getPublicKey());

        try {
            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            socketChannel.connect(new InetSocketAddress(connectInfo.getAddress(), connectInfo.getPort()));
        } catch (IOException e) {
            if (socketChannel != null) {
                try { socketChannel.close(); } catch (IOException t) {}
            }
            if (selector != null) {
                try { selector.close(); } catch (IOException t) {}
            }
            throw e;
        }

        this.selector = selector;
        this.socketChannel = socketChannel;
    }

    public static TcpChannelClient connect(TcpConnectInfo connectInfo, EphemeralKeyPair myKey, IpcChannelListener ipcChannelListener) throws IOException {
        return new TcpChannelClient(connectInfo, myKey, ipcChannelListener);
    }

    public IpcChannel getIpcChannel() {
        return this.ipcChannel;
    }

    public void run() throws IOException {
        ByteBuffer receivingBuffer = FrameDecoder.createBuffer(16777216);
        ((Buffer) receivingBuffer).clear();

        FrameDecoder frameDecoder = new FrameDecoder(receivingBuffer);

        try {
            boolean running = true;
            while (running) {
                int selectedCount = this.selector.select();
                if (selectedCount > 0) {
                    Iterator<SelectionKey> selectedKeys = this.selector.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        SelectionKey selectionKey = selectedKeys.next();
                        if (!selectionKey.isValid()) {
                            selectedKeys.remove();
                            continue;
                        }

                        if (selectionKey.isConnectable()) {
                            boolean finishConnect = this.socketChannel.finishConnect();
                            if (finishConnect) {
                                this.socketChannel.register(this.selector, SelectionKey.OP_READ);
                                this.ipcChannel.sendClientHello();
                            }
                        } else {
                            if (selectionKey.isReadable()) {
                                int readLength = this.socketChannel.read(receivingBuffer);
                                if (readLength == 0) {
                                    running = false;
                                    break;
                                }
                                frameDecoder.recvAfterReadRaw(this.ipcChannel);
                            }
                        }
                        selectedKeys.remove();
                    }
                }
                while (!this.commandQueue.isEmpty()) {
                    int command = this.commandQueue.pop();
                    if (command == COMMAND_CLOSE) {
                        if (this.socketChannel.isConnected()) {
                            this.socketChannel.close();
                        }
                        running = false;
                    }
                }
            }
        } catch (AsynchronousCloseException closeException) {
            return;
        }

        this.cleanup();
    }

    public void close() {
        System.err.println("[SIPC CLIENT] DO CLOSE");

        this.commandQueue.push(COMMAND_CLOSE);
    }

    private void cleanup() {
        System.err.println("[SIPC CLIENT] CLEANUP");

        if (this.socketChannel.isOpen()) {
            try { this.socketChannel.close(); } catch (IOException t) {}
        }
        if (this.selector.isOpen()) {
            try { this.selector.close(); } catch (IOException t) {}
        }
    }
}
