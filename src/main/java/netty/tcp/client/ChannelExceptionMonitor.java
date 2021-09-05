package netty.tcp.client;


import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ChannelExceptionMonitor extends ChannelInboundHandlerAdapter {
    private final ChannelExceptionListener channelExceptionListener;

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelExceptionListener.channelInactive();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        channelExceptionListener.exceptionCaught(cause);
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }
}
