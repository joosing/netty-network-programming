package netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.extern.slf4j.Slf4j;
import netty.util.ChannelEventLoopTask;

import java.net.InetSocketAddress;


@Slf4j
public class UdpChannel {
    private final ChannelEventLoopTask channelEventLoopTask;
    private final Channel channel;

    public UdpChannel(InetSocketAddress localAddress, ChannelInitializer<NioDatagramChannel> channelInitializer) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        channel = bootstrap.group(eventLoopGroup)
                .channel(NioDatagramChannel.class)
                .handler(channelInitializer)
                .bind(localAddress.getPort()).sync().channel();

        channelEventLoopTask = new ChannelEventLoopTask(channel);
    }

    public void destroy() {
        try {
            channel.eventLoop().shutdownGracefully().sync();
            channelEventLoopTask.stopAll();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean send(Object message) {
        try {
            var channelFuture = channel.writeAndFlush(message);
        } catch (Exception e) {
            System.out.println(toErrorMessage("전송 실패", e));
            return false;
        }
        return true;
    }

    public ChannelEventLoopTask channelEventLoopTask() {
        return channelEventLoopTask;
    }

    private String toErrorMessage(String description, Throwable e) {
        log.error(e.getMessage(), e);
        return String.format("%s (%s:%d)", description);
    }
}
