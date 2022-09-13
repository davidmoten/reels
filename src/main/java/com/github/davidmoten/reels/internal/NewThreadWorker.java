package com.github.davidmoten.reels.internal;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public class NewThreadWorker implements Worker {

    private final ScheduledExecutorService executor;
    private volatile boolean disposed;

    public NewThreadWorker(ThreadFactory threadFactory) {
        executor = createExecutor(threadFactory);
    }

    @Override
    public void dispose() {
        disposed = true;
        executor.shutdownNow();
    }

    @Override
    public Disposable schedule(Runnable run) {
        if (disposed) {
            return Disposable.NOOP;
        }
        return new FutureTask(executor.submit(run));
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (disposed) {
            return Disposable.NOOP;
        }
        return new FutureTask(executor.schedule(run, delay, unit));
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        if (disposed) {
            return Disposable.NOOP;
        }
        return new FutureTask(executor.scheduleAtFixedRate(run, initialDelay, period, unit));
    }

    private static ScheduledExecutorService createExecutor(ThreadFactory factory) {
        final ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, factory);
        exec.setRemoveOnCancelPolicy(true);
        return exec;
    }
    
    @Override
    public boolean isDisposed() {
        return disposed;
    }

}
