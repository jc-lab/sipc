package kr.jclab.sipc.internal;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.function.Consumer;

public class InactiveHandler extends ChannelInboundHandlerAdapter {
    private final Consumer<ChannelHandlerContext> handler;

    public InactiveHandler(Consumer<ChannelHandlerContext> handler) {
        this.handler = handler;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            super.channelInactive(ctx);
        } finally {
            this.handler.accept(ctx);
        }
    }
}
