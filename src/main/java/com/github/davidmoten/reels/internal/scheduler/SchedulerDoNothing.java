package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public enum SchedulerDoNothing implements Scheduler {

    INSTANCE;

    @Override
    public Worker createWorker() {
        return WorkerDisposed.INSTANCE;
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

    @Override
    public void shutdown() {
        // do nothing
    }
    
    @Override
    public boolean requiresSynchronization() {
        return false;
    }

}
