package com.github.davidmoten.reels.internal;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public final class ImmediateWorker implements Worker {

    private volatile boolean disposed;

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public Disposable schedule(Runnable run) {
        if (!disposed) {
            run.run();
        }
        return Disposable.NOOP;
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (!disposed) {
            try {
                unit.sleep(delay);
            } catch (InterruptedException e) {
                return Disposable.NOOP;
            }
        }
        if (!disposed) {
            run.run();
        }
        return Disposable.NOOP;
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }
}
