package netty.udp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.InetSocketAddress;

public class UdpRemoteAgency extends ChannelInboundHandlerAdapter {
    private InetSocketAddress remoteAddress;
    private final PropertyChangeSupport support;

    public UdpRemoteAgency() {
        support = new PropertyChangeSupport(this);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        var datagram = (DatagramPacket) msg;
        InetSocketAddress newRemoteAddress = datagram.sender();
        support.firePropertyChange("Remote", remoteAddress, newRemoteAddress);
        remoteAddress = newRemoteAddress;
        super.channelRead(ctx, msg);
    }

    public void addRemoteChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removeRemoteChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
}

