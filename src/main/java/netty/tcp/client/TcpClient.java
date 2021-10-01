package netty.tcp.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import netty.util.ChannelEventLoopTask;
import org.springframework.util.Assert;

import java.net.InetSocketAddress;
import java.util.concurrent.*;

/**
 * Netty Tcp Client 기능을 제공합니다.
 */
public class TcpClient implements ChannelExceptionListener {
    private final Bootstrap bootstrap = new Bootstrap();
    private final TcpClient.ConnectUntilSuccess connectUntilSuccess = new ConnectUntilSuccess();
    private ChannelEventLoopTask eventLoopTasks;
    private Channel channel;
    private String triedIp;
    private int triedPort;
    private boolean shouldRecoverConnect = true;
    private boolean shouldAlarmConnectFail = true;
    private final int connectTimeoutMillis = 3000;

    public void init(ChannelInitializer<?> channelInitializer) {
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMillis)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(channelInitializer);
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

    public boolean connect(String tryingIp, int tryingPort) {
        shouldAlarmConnectFail = true;
        disconnect();
        return connectOnce(tryingIp, tryingPort);
    }

    public boolean connectUntilSuccess(String tryingIp, int tryingPort) {
        shouldAlarmConnectFail = true;
        disconnect();
        return connectUntilSuccess.sync(tryingIp, tryingPort);
    }

    public Future<Void> beginConnectUntilSuccess(String tryingIp, int tryingPort) {
        shouldAlarmConnectFail = true;
        disconnect();
        return connectUntilSuccess.begin(tryingIp, tryingPort);
    }

    private synchronized boolean connectOnce(String tryingIp, int tryingPort) {
        ChannelFuture channelFuture;
        try {
            channelFuture = bootstrap.connect(tryingIp, tryingPort).sync();
        } catch (Exception e) {
            if (shouldAlarmConnectFail) {
                System.out.println(toErrorMessage("trying to connect fails", tryingIp, tryingPort, e));
                shouldAlarmConnectFail = false;
            }
            return false;
        } finally {
            this.triedIp = tryingIp;
            this.triedPort = tryingPort;
        }

        if (!channelFuture.isSuccess()) {
            return false;
        }

        shouldAlarmConnectFail = true;
        shouldRecoverConnect = true;
        channel = channelFuture.channel();
        eventLoopTasks = new ChannelEventLoopTask(channel);
        channel.pipeline().addLast(new ChannelExceptionMonitor(this));
        System.out.println("Connected");
        return true;
    }

    public void disconnect() {
        shouldRecoverConnect = false; // 명시적으로 연결을 끊는 경우 연결 복구 로직 OFF
        try {
            connectUntilSuccess.stop();
            stopEventLoopTasks();
            if (channel != null) {
                channel.close().sync();
                channel = null;
            }
        } catch (Exception e) {
            System.out.println(toErrorMessage("trying to disconnect fails", triedIp, triedPort, e));
        }
    }

    public boolean send(Object message) {
        Assert.notNull(triedIp, "connectUntilSuccess() must be called before.");
        if (channel == null) {
            System.out.println(toErrorMessage("trying to send fails", triedIp, triedPort, new NullPointerException()));
            return false;
        }

        try {
            channel.writeAndFlush(message);
        } catch (Exception e) {
            System.out.println(toErrorMessage("trying to send fails", triedIp, triedPort, e));
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

    public boolean isActive() {
        if (channel == null) {
            return false;
        }
        return channel.isActive();
    }

    public InetSocketAddress getLocalAddress() {
        if (channel == null) {
            return null;
        }
        return (InetSocketAddress) channel.localAddress();
    }

    @Override
    public void channelInactive() {
        System.out.println("Disconnected");
        if (shouldRecoverConnect) {
            connectUntilSuccess.begin(this.triedIp, triedPort);
        }
    }

    @Override
    public void exceptionCaught(Throwable cause) {
        System.out.println(toErrorMessage("Channel exception caught", triedIp, triedPort, cause));
    }

    private String toErrorMessage(String description, String ip, int port, Throwable e) {

        return String.format("%s (%s, %d, %s)", description, ip, port, e.getMessage());
    }

    /**
     * 전용 쓰레드를 통해 연결 성공할 때 까지 연결을 재시도하는 기능을 캡슐화합니다.
     * Netty 에서 제공하는 EventLoop 를 통하여 실행 시, I/O 작업에 영향을 끼치는 동기 함수 사용에 제약이 생겨 별도의 전용 쓰레드로 처리합니다.
     */
    private class ConnectUntilSuccess {
        /**
         * 연결 반복 실행 전용 쓰레드
         */
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        /**
         * 비동기 연결 반복 실행에 대한 Future 객체
         */
        private Future<Void> future;
        /**
         * 연결 반복 실행 종료
         */
        private CountDownLatch cancelEvent;

        /**
         * 연결 반복 실행 동기화 수행
         *
         * @param tryingIp IP
         * @param tryingPort 포트
         * @return 연결 결과
         */
        public boolean sync(String tryingIp, int tryingPort) {
            future = begin(tryingIp, tryingPort);
            try {
                future.get();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        /**
         * 연결 반복 실행 태스크 시작
         *
         * @param tryingIp IP
         * @param tryingPort 포트
         * @return 연결 반복 실행에 대한 Future 객체
         * @see this.connectOnce()
         */
        public Future<Void> begin(String tryingIp, int tryingPort) {
            cancelEvent = new CountDownLatch(1);
            future = executor.submit(() -> {
                boolean connected;
                do {
                    if (cancelEvent.await(100, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                    connected = TcpClient.this.connectOnce(tryingIp, tryingPort);
                } while (!connected);
                cancelEvent.countDown();
                return null; // TODO 이건 뭘 의미하는 거지?
            });
            return future;
        }

        /**
         * 연결 반복 실행 종료
         */
        public void stop() {
            final int extraStopTimeoutMillis = 500;
            if (cancelEvent != null && cancelEvent.getCount() > 0) {
                cancelEvent.countDown();
                try {
                    future.get(connectTimeoutMillis + extraStopTimeoutMillis, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
