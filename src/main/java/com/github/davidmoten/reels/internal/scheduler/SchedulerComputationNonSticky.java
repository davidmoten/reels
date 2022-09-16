package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.Util;

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

}
