package kr.jclab.sipc.client.internal;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import kr.jclab.sipc.client.SipcClient;
import kr.jclab.sipc.internal.EventLoopHolder;
import kr.jclab.sipc.proto.SipcProto;

import java.net.InetSocketAddress;
import java.net.URI;

public class SipcTcpClient extends SipcClient {
    public SipcTcpClient(
            EventLoopHolder eventLoopHolder,
            SipcProto.ConnectInfo connectInfo,
            ChannelHandler handler
    ) {
        super(eventLoopHolder, connectInfo, handler);
    }

    @Override
    protected Channel createChannel() {
        Bootstrap bootstrap = new Bootstrap();
        if (!eventLoopHolder.isGroupPresent()) {
            eventLoopHolder.initialize(null, new NioEventLoopGroup(1));
        }
        bootstrap = bootstrap.channel(NioSocketChannel.class);

        URI uri = URI.create("tcp://" + clientContext.getConnectInfo().getTransportAddress());

        return bootstrap
                .group(eventLoopHolder.getWorker())
                .handler(clientChannelInitializer)
                .connect(new InetSocketAddress(uri.getHost(), uri.getPort()))
                .channel();
    }
}
