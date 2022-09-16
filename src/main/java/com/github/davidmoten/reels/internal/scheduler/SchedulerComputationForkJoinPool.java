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
public final class SchedulerComputationForkJoinPool implements Scheduler {

    public static final SchedulerComputationForkJoinPool INSTANCE = new SchedulerComputationForkJoinPool();

    private final ScheduledExecutorService executor;
    private final ExecutorWorker worker;

    private SchedulerComputationForkJoinPool() {
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
