package netty.udp;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import netty.util.EventLoopTasks;

public class UdpClient {
    private final Bootstrap bootstrap = new Bootstrap();
    private EventLoopTasks eventLoopTasks;
    private Channel channel;
}
