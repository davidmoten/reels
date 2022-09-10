package com.github.davidmoten.reels.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;

public final class SchedulerImmediate implements Scheduler {

    public static final Scheduler INSTANCE = new SchedulerImmediate();

    private final ScheduledExecutorService scheduled;

    private SchedulerImmediate() {
        this.scheduled = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public Disposable schedule(Runnable run) {
        run.run();
        return Disposable.NOOP;
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        Future<?> future = scheduled.schedule(run, delay, unit);
        return new FutureTask(future);
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        Future<?> future = scheduled.scheduleWithFixedDelay(run, initialDelay, period, unit);
        return new FutureTask(future);
    }

}
