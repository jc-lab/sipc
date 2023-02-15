package kr.jclab.sipc.internal;

import io.netty.channel.EventLoopGroup;
import lombok.Getter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Getter
public class EventLoopHolder {
    private EventLoopGroup boss = null;
    private EventLoopGroup worker = null;
    private boolean groupOwned = false;
    private ScheduledExecutorService scheduledExecutorService = null;
    private boolean scheduledExecutorServiceOwned = false;
    private boolean shutdown = false;

    public boolean isGroupPresent() {
        return worker != null;
    }

    public boolean isScheduledExecutorServicePresent() {
        return scheduledExecutorService != null;
    }

    public void useExternal(EventLoopGroup boss, EventLoopGroup worker) {
        this.boss = boss;
        this.worker = worker;
        groupOwned = false;
    }

    public void useExternal(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        scheduledExecutorServiceOwned = false;
    }

    public void initialize(EventLoopGroup boss, EventLoopGroup worker) {
        this.boss = boss;
        this.worker = worker;
        groupOwned = true;
    }

    public void initialize(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        scheduledExecutorServiceOwned = true;
    }

    public synchronized ScheduledExecutorService getScheduledExecutorService() {
        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
            scheduledExecutorServiceOwned = true;
        }
        return scheduledExecutorService;
    }

    public synchronized void shutdown() {
        if (shutdown) {
            return ;
        }
        shutdown = true;
        if (groupOwned) {
            if (boss != null) {
                boss.shutdownGracefully();
            }
            worker.shutdownGracefully();
        }
        if (scheduledExecutorServiceOwned) {
            if (scheduledExecutorService != null) {
                scheduledExecutorService.shutdown();
            }
        }
    }
}
