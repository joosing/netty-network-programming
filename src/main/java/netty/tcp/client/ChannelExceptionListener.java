package netty.tcp.client;

public interface ChannelExceptionListener {
    /**
     * 채널 Inactive 이벤트 발생
     */
    void channelInactive();

    /**
     * 채널 Exception 이벤트 발생
     */
    void exceptionCaught(Throwable cause);
}
