package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.Util;

/**
 * This class present for benchmark purposes. SchedulerComputation is more thatn
 * twice as fast on parallel benchmarks so should be used all the time.
 */
public final class SchedulerComputationNonSticky implements Scheduler {

    public static final SchedulerComputationNonSticky INSTANCE = new SchedulerComputationNonSticky();

    private final ScheduledExecutorService executor;
    private final ExecutorWorker worker;

    private SchedulerComputationNonSticky() {
        int size = Integer.getInteger("reels.computation.pool.size", Runtime.getRuntime().availableProcessors());
        executor = Executors.newScheduledThreadPool(size, Util.createThreadFactory("ReelsComputationNonSticky"));
        // this worker will not be constrained to a single thread
        // message ordering to an actor should still be maintained
        worker = new ExecutorWorker(executor);
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
        return new FutureTask(executor.schedule(run, delay, unit));
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        return new FutureTask(executor.scheduleAtFixedRate(run, initialDelay, period, unit));
    }

}
