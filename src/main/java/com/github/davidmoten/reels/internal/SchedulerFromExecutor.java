package com.github.davidmoten.reels.internal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;

public final class SchedulerFromExecutor implements Scheduler {

    private final ExecutorService direct;
    private final ScheduledExecutorService scheduled;

    public SchedulerFromExecutor(ExecutorService direct, ScheduledExecutorService scheduled) {
        this.direct = direct;
        this.scheduled = scheduled;
    }

    @Override
    public Disposable schedule(Runnable run) {
        Future<?> future = direct.submit(run);
        return new FutureTask(future);
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        Future<?> future = scheduled.schedule(run, delay, unit);
        return new FutureTask(future);
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long interval, TimeUnit unit) {
        Future<?> future = scheduled.scheduleWithFixedDelay(run, initialDelay, interval, unit);
        return new FutureTask(future);
    }

}
