package com.github.davidmoten.reels;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class SchedulerComputation implements Scheduler {

    public static final Scheduler INSTANCE = null;

    private final SchedulerFromExecutor scheduler;

    private SchedulerComputation() {
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(
                Util.systemPropertyInt("reels.computation.pool.size", Runtime.getRuntime().availableProcessors()));
        scheduler = new SchedulerFromExecutor(executor);
    }

    @Override
    public Disposable schedule(Runnable run) {
        return scheduler.schedule(run);
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        return scheduler.schedule(run, delay, unit);
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long interval, TimeUnit unit) {
        return scheduler.schedulePeriodically(run, initialDelay, interval, unit);
    }

}
