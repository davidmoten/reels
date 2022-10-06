package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public final class SchedulerWorker extends AbstractCanScheduleDisposable implements Worker {

    private final Worker worker;
    private volatile boolean disposed;

    public SchedulerWorker(Worker worker) {
        this.worker = worker;
    }

    @Override
    public Disposable _schedule(Runnable run) {
        return worker.schedule(run);
    }

    @Override
    public Disposable _schedule(Runnable run, long delay, TimeUnit unit) {
        return worker.schedule(run, delay, unit);
    }

    @Override
    public Disposable _schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
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
