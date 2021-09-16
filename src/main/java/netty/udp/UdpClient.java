package netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import netty.util.EventLoopTasks;
import java.util.concurrent.TimeUnit;


public class UdpClient {
    private final Bootstrap bootstrap = new Bootstrap();
    private EventLoopTasks eventLoopTasks;
    private Channel channel;
    private String remoteIp;
    private int remotePort;

    public void init(ChannelInitializer<?> channelInitializer, String remoteIp, int remotePort) throws InterruptedException {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        channel = bootstrap.group(eventLoopGroup)
                .channel(NioDatagramChannel.class)
                .handler(channelInitializer)
                .bind(remoteIp, remotePort).sync().channel();

        this.remoteIp = remoteIp;
        this.remotePort = remotePort;

        eventLoopTasks = new EventLoopTasks(channel);
    }

    public void destroy() {
        try {
            if (channel != null && channel.eventLoop() != null) {
                channel.eventLoop().shutdownGracefully().sync();
            }
            eventLoopTasks.stopAll();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public boolean send(Object message) {
        if (channel == null) {
            System.out.println(toErrorMessage("trying to send fails", remoteIp, remotePort, new NullPointerException()));
            return false;
        }

        try {
            channel.writeAndFlush(message);
        } catch (Exception e) {
            System.out.println(toErrorMessage("trying to send fails", remoteIp, remotePort, e));
            return false;
        }
        return true;
    }

    public boolean scheduleEventLoopTask(Runnable task, long initialDelay, long period, TimeUnit unit) {
        return eventLoopTasks.schedule(task, initialDelay, period, unit);
    }

    public void stopEventLoopTasks() {
        eventLoopTasks.stopAll();
    }

    private String toErrorMessage(String description, String ip, int port, Throwable e) {

        return String.format("%s (%s, %d, %s)", description, ip, port, e.getMessage());
    }
}
