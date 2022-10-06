package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public class ExecutorWorker extends AbstractCanScheduleDisposable implements Worker {
    private volatile boolean disposed;
    private final ScheduledExecutorService executor;

    public ExecutorWorker(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    protected Disposable _schedule(Runnable run) {
        return new FutureTask(executor.submit(run));
    }

    @Override
    protected Disposable _schedule(Runnable run, long delay, TimeUnit unit) {
        return new FutureTask(executor.schedule(run, delay, unit));
    }

    @Override
    protected Disposable _schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        return new FutureTask(executor.scheduleAtFixedRate(run, initialDelay, period, unit));
    }
}