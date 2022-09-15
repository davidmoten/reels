package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public class ExecutorWorker implements Worker {
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
    public Disposable schedule(Runnable run) {
        if (disposed) {
            return Disposable.DISPOSED;
        } else {
            return new FutureTask(executor.submit(run));
        }
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (disposed) {
            return Disposable.DISPOSED;
        } else {
            return new FutureTask(executor.schedule(run, delay, unit));
        }
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        if (disposed) {
            return Disposable.DISPOSED;
        } else {
            return new FutureTask(executor.scheduleAtFixedRate(run, initialDelay, period, unit));
        }
    }
}