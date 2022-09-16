package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

/**
 * This class present for benchmark purposes. SchedulerComputation is more thatn
 * twice as fast on parallel benchmarks so should be used all the time.
 */
public final class SchedulerForkJoinPool implements Scheduler {

    public static final SchedulerForkJoinPool INSTANCE = new SchedulerForkJoinPool();

    private final ScheduledExecutorService executor;
    private final ExecutorWorker worker;

    private SchedulerForkJoinPool() {
        executor = new PartialScheduledExecutorService(ForkJoinPool.commonPool(),
                Executors.newSingleThreadScheduledExecutor());
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
