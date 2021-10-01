package netty.udp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageEncoder;
import lombok.extern.slf4j.Slf4j;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
public class StringToDatagramEncoder extends MessageToMessageEncoder<String> implements PropertyChangeListener {
    /**
     * UDP 통신에서 원격지는 고정적으로 바인딩되거나(Client, or Peer to Peer) 또는 원격지로부터 메시지를 받고 난 후 동적으로 바인딩(Server) 될 수 있습니다.
     * 따라서 원격지 주소는 없을 수 있습니다.
     */
    private Optional<InetSocketAddress> optRemoteAddress;

    public StringToDatagramEncoder() {
        optRemoteAddress = Optional.empty();
    }

    public StringToDatagramEncoder(InetSocketAddress remoteAddress) {
        optRemoteAddress = Optional.of(remoteAddress);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, String msg, List<Object> out) throws IllegalStateException {
        optRemoteAddress.orElseThrow(() -> new IllegalStateException("원격지 정보가 바인딩되지 않았습니다."));

        // TODO : 할당한 ByteBuf는 어디에서 해제해 주는 것이 좋은가? (기술부채)
        ByteBuf byteBuf = Unpooled.copiedBuffer(msg, StandardCharsets.UTF_8);
        out.add(new DatagramPacket(byteBuf, optRemoteAddress.get()));

        log.debug("Write : " + byteBuf.toString());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        optRemoteAddress = Optional.of((InetSocketAddress) evt.getNewValue());
    }
}
