package com.github.davidmoten.reels.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;

public final class SchedulerComputation implements Scheduler {

    public static final Scheduler INSTANCE = new SchedulerComputation();

    private final SchedulerFromExecutor scheduler;

    private SchedulerComputation() {
        int size = Util.systemPropertyInt("reels.computation.pool.size", Runtime.getRuntime().availableProcessors());
        ExecutorService direct = Executors.newWorkStealingPool(size);
        ScheduledExecutorService scheduled = Executors.newSingleThreadScheduledExecutor();
        scheduler = new SchedulerFromExecutor(direct, scheduled);
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
