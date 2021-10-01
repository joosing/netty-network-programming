package netty.util;

import io.netty.channel.Channel;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Channel 의 EventLoop 쓰레드를 통해 실행할 사용자 태스크를 처리합니다.
 */
public class ChannelEventLoopTask {
    private final Channel channel;
    private final CopyOnWriteArrayList<ScheduledFuture<?>> userTaskFutures = new CopyOnWriteArrayList<>();

    public ChannelEventLoopTask(Channel channel ) {
        this.channel = channel;
    }

    public boolean schedule(Runnable task, long initialDelay, long period, TimeUnit unit) {
        if (channel == null) {
            System.out.println("trying to schedule fails on " + channel.remoteAddress().toString());
            return false;
        }
        ScheduledFuture<?> future = channel.eventLoop().scheduleAtFixedRate(task, initialDelay, period, unit);
        userTaskFutures.add(future);
        return true;
    }

    public void stopAll() {
        if (!userTaskFutures.isEmpty()) {
            userTaskFutures.forEach(future -> future.cancel(true));
            userTaskFutures.clear();
        }
    }
}
