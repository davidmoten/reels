package com.github.davidmoten.reels.internal;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;

public final class SchedulerFromExecutor implements Scheduler {

    private final ScheduledExecutorService executor;

    public SchedulerFromExecutor(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public Disposable schedule(Runnable run) {
        Task task = new Task(run);
        executor.execute(task);
        return task;
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        Future<?> future = executor.schedule(run, delay, unit);
        return new FutureTask(future);
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long interval, TimeUnit unit) {
        Future<?> future = executor.scheduleWithFixedDelay(run, initialDelay, interval, unit);
        return new FutureTask(future);
    }

}
