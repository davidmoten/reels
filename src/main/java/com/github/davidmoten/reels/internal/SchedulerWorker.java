package com.github.davidmoten.reels.internal;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public final class SchedulerWorker implements Worker {

    private final Worker worker;
    private volatile boolean disposed;
    
    public SchedulerWorker(Worker worker) {
        this.worker = worker;
    }

    @Override
    public Disposable schedule(Runnable run) {
        if (disposed) {
            return Disposable.NOOP;
        }
        return worker.schedule(run);
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (disposed) {
            return Disposable.NOOP;
        }
        return worker.schedule(run, delay, unit);
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        if (disposed) {
            return Disposable.NOOP;
        }
        return worker.schedulePeriodically(run, initialDelay, period, unit);
    }
    
    @Override
    public void dispose() {
        disposed = true;
    }
    
    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
