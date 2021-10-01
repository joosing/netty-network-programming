package netty.tcp.udp;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import lombok.SneakyThrows;
import netty.udp.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UdpTest {
    InetSocketAddress serverAddress = new InetSocketAddress("127.0.0.1", 12345);
    InetSocketAddress clientAddress = new InetSocketAddress("127.0.0.1", 54321);

    @BeforeEach
    void beforeEach() {

    }

    @Test
    @SneakyThrows
    void udpClientDecoderTest() {
        final String sampleData = "Inbound Handler Test Data";

        EmbeddedChannel testChannel = new EmbeddedChannel(
                new DatagramToStringDecoder()
        );

        assertTrue(testChannel.writeInbound(new DatagramPacket(Unpooled.copiedBuffer(sampleData, StandardCharsets.UTF_8), serverAddress)));
        assertEquals(sampleData, (String) testChannel.readInbound());
    }

    @Test
    @SneakyThrows
    void udpServerDecoderTest() {
        final String sampleData = "Inbound Handler Test Data";

        StringToDatagramEncoder stringToDatagramEncoder = new StringToDatagramEncoder();
        UdpRemoteAgency udpRemoteAgency = new UdpRemoteAgency();
        udpRemoteAgency.addRemoteChangeListener(stringToDatagramEncoder);

        EmbeddedChannel testChannel = new EmbeddedChannel(
                udpRemoteAgency,
                new DatagramToStringDecoder()
//                new UdpEchoHandler()
        );

        assertTrue(testChannel.writeInbound(new DatagramPacket(Unpooled.copiedBuffer(sampleData, StandardCharsets.UTF_8), serverAddress, clientAddress)));
        assertEquals(sampleData, (String) testChannel.readInbound());
    }

    @Test
    @SneakyThrows
    void udpConnectionTest() {

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
