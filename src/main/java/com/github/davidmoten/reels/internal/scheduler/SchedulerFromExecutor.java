package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public class SchedulerFromExecutor implements Scheduler {

    private final ScheduledExecutorService executor;
    private final ExecutorWorker worker;
    private final boolean requiresSerialization;

    public SchedulerFromExecutor(ScheduledExecutorService executor, boolean requiresSerialization) {
        this.executor = executor;
        this.requiresSerialization = requiresSerialization;
        // this worker will not necessarily be constrained to a single thread,
        // message ordering to an actor should still be maintained due
        this.worker = new ExecutorWorker(executor);
    }

    @Override
    public Worker createWorker() {
        return new SchedulerWorker(worker);
    }

    @Override
    public void shutdown() {
        worker.dispose();
        executor.shutdownNow();
    }

    @Override
    public Disposable schedule(Runnable run) {
        return new FutureTask(executor.submit(run));
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (delay <= 0) {
            return schedule(run);
        } else {
            return new FutureTask(executor.schedule(run, delay, unit));
        }
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        return new FutureTask(executor.scheduleAtFixedRate(run, initialDelay, period, unit));
    }

    @Override
    public boolean requiresSerialization() {
        return requiresSerialization;
    }
}
