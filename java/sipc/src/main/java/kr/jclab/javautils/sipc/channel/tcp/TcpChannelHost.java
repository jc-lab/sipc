package kr.jclab.javautils.sipc.channel.tcp;

import kr.jclab.javautils.sipc.bson.entity.TcpConnectInfo;
import kr.jclab.javautils.sipc.channel.ChannelHost;
import kr.jclab.javautils.sipc.DefaultChannelType;
import kr.jclab.javautils.sipc.channel.IpcChannel;
import kr.jclab.javautils.sipc.channel.IpcChannelListener;
import kr.jclab.javautils.sipc.crypto.EphemeralKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class TcpChannelHost implements ChannelHost {
    private final Logger log = LoggerFactory.getLogger(TcpChannelHost.class);

    private final ServerSocketChannel serverSocketChannel;
    private final InetSocketAddress serverAddress;

    private final CompletableFuture<Void> threadInitFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> closeFuture = new CompletableFuture<>();
    private final Thread workerThread = new Thread(this::workerRun);

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Selector selector;

    private final Map<String, IpcChannel> channelMap = new HashMap<>();

    public static TcpChannelHost create(InetSocketAddress inetSocketAddress) throws IOException {
        return new TcpChannelHost(inetSocketAddress);
    }

    public static TcpChannelHost create() throws IOException {
        return new TcpChannelHost(null);
    }

    private TcpChannelHost(InetSocketAddress inetSocketAddress) throws IOException {
        ServerSocketChannel serverSocketChannel = null;
        Selector selector = null;

        try {
            serverSocketChannel = ServerSocketChannel.open();
            selector = Selector.open();

            this.serverSocketChannel = serverSocketChannel;
            this.selector = selector;

            InetAddress address = Optional.ofNullable(inetSocketAddress)
                    .map(InetSocketAddress::getAddress)
                    .orElse(InetAddress.getAllByName("127.0.0.2")[0]);
            int port = (inetSocketAddress != null) ? inetSocketAddress.getPort() : 0;
            InetSocketAddress serverAddress = null;

            SocketException bindException = null;
            if (port > 0) {
                serverAddress = new InetSocketAddress(address, port);
                serverSocketChannel.bind(serverAddress);
            } else {
                for (port = 65000; port > 4096; port--) {
                    try {
                        if (port == 8080) continue;
                        InetSocketAddress tempAddress = new InetSocketAddress(address, port);
                        this.serverSocketChannel.bind(tempAddress);
                        serverAddress = tempAddress;
                        break;
                    } catch (SocketException e) {
                        bindException = e;
                    }
                }
            }
            if (serverAddress == null) {
                throw new BindException("No idle ports: " + bindException.getMessage());
            }

            this.serverAddress = serverAddress;
            this.log.info("bind on " + serverAddress.toString());

            serverSocketChannel.configureBlocking(false);

            this.running.set(true);
            this.workerThread.start();

            try {
                threadInitFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                this.running.set(false);
                if (e instanceof ExecutionException) {
                    throw new IOException(e.getCause());
                }
                throw new IOException(e);
            }
        } catch (IOException e) {
            if (selector != null) {
                try { selector.close(); } catch (IOException ignoredException) { /* IGNORE */ }
            }
            if (serverSocketChannel != null) {
                try { serverSocketChannel.close(); } catch (IOException ignoredException) { /* IGNORE */ }
            }
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        this.running.set(false);
        try { this.selector.close(); } catch (IOException ignoredException) { /* IGNORE */ }
        try {
            this.closeFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    private void workerRun() {
        SelectionKey acceptKey;

        try {
            int ops = serverSocketChannel.validOps();
            acceptKey = this.serverSocketChannel.register(this.selector, ops, null);
        } catch (IOException e) {
            threadInitFuture.completeExceptionally(e);
            return ;
        }
        threadInitFuture.complete(null);

        while (this.running.get()) {
            try {
                int count = this.selector.select();
                if (count <= 0) continue;

                Set<SelectionKey> keys = this.selector.selectedKeys();

                for (Iterator<SelectionKey> iterator = keys.iterator(); iterator.hasNext(); iterator.remove()) {
                    SelectionKey key = iterator.next();

                    if (!key.isValid()) {
                        this.log.info("invalid key: " + key);
                        continue;
                    }

                    if (key.isAcceptable()) {
                        SocketChannel clientChannel = serverSocketChannel.accept();
                        TcpClientContext clientContext = new TcpClientContext(this, clientChannel);
                        clientChannel.configureBlocking(false);
                        clientChannel.register(this.selector, SelectionKey.OP_READ, clientContext);
                        this.log.info("client accepted: " + clientChannel);
                        continue;
                    }

                    if (key.isReadable()) {
                        SocketChannel clientChannel = (SocketChannel) key.channel();
                        TcpClientContext clientContext = (key.attachment() instanceof TcpClientContext)
                                ? (TcpClientContext)key.attachment() :
                                null;
                        boolean needCleanup = false;

                        try {
                            if (clientContext == null) {
                                throw new IOException("Something wrong");
                            }

                            if (!clientContext.doRead()) {
                                this.log.info("Client[" + clientChannel + "] close");
                                needCleanup = true;
                                clientChannel.close();
                            }
                        } catch (Throwable e) {
                            needCleanup = true;
                            this.log.warn("Client[" + clientChannel + "] error: ", e);
                            try { clientChannel.close(); } catch (IOException ignoreEx) {}
                        }
                        if (clientContext != null && needCleanup) {
                            clientContext.cleanup();
                        }
                    }
                }
            } catch (IOException e) {
                log.warn("ERROR", e);
            }
        }

        this.closeFuture.complete(null);
    }

    @Override
    public String getType() {
        return DefaultChannelType.Tcp4.value();
    }

    @Override
    public IpcChannel createChannel(IpcChannelListener ipcChannelListener, EphemeralKeyPair serverKey) {
        TcpIpcChannel channel = new TcpIpcChannel(
                ipcChannelListener,
                serverKey,
                TcpConnectInfo.CHANNEL_TYPE_TCP4,
                this.serverAddress.getHostString(),
                this.serverAddress.getPort()
        );
        this.channelMap.put(channel.getChannelId(), channel);
        return channel;
    }

    IpcChannel findIpcChannel(String channelId) {
        return this.channelMap.get(channelId);
    }
}
