package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public final class ImmediateWorker extends AbstractCanScheduleDisposable implements Worker {

    private volatile boolean disposed;

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
    
    @Override
    public Disposable _schedule(Runnable run) {
        run.run();
        return Disposable.disposed();
    }

    @Override
    public Disposable _schedule(Runnable run, long delay, TimeUnit unit) {
        try {
            unit.sleep(delay);
        } catch (InterruptedException e) {
            return Disposable.disposed();
        }
        if (!disposed) {
            run.run();
        }
        return Disposable.disposed();
    }

    @Override
    public Disposable _schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException("cannot schedule periodically with the immediate scheduler");
    }
}
