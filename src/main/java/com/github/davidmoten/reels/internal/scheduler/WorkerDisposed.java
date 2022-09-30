package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public enum WorkerDisposed implements Worker {
    
    INSTANCE;
    
    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public boolean isDisposed() {
        return true;
    }

    @Override
    public Disposable schedule(Runnable run) {
        return Disposable.disposed();
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        return Disposable.disposed();
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        return Disposable.disposed();
    }

}
