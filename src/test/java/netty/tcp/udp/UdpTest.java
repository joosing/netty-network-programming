package netty.tcp.udp;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.SneakyThrows;
import netty.udp.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetSocketAddress;

@SpringBootTest
class UdpTest {

    @BeforeEach
    void beforeEach() {

    }

    @Test
    @SneakyThrows
    void udpConnectionTest() {
        InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 12345);
        InetSocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 54321);

        UdpChannel client = new UdpChannel(clientAddress, new ChannelInitializer<>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                ch.pipeline()
                        // Decoder
                        .addLast(new DatagramToStringDecoder())

                        // Encoder
                        .addLast(new StringToDatagramEncoder(serverAddress));
            }
        });

        UdpChannel server = new UdpChannel(serverAddress, new ChannelInitializer<>() {
            @Override
            protected void initChannel(NioDatagramChannel ch) {
                StringToDatagramEncoder stringToDatagramEncoder = new StringToDatagramEncoder();
                UdpRemoteAgency udpRemoteAgency = new UdpRemoteAgency();
                udpRemoteAgency.addRemoteChangeListener(stringToDatagramEncoder);

                ch.pipeline()
                        // Decoder
                        .addLast(udpRemoteAgency)
                        .addLast(new DatagramToStringDecoder())
                        .addLast(new UdpEchoHandler())

                        // Encoder
                        .addLast(stringToDatagramEncoder);
            }
        });

        client.send("SimpleData");
        Thread.sleep(100000);

    }
}
