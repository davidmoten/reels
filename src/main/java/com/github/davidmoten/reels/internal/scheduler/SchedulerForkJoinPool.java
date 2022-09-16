package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

/**
 * This class present for benchmark purposes. SchedulerComputation is more thatn
 * twice as fast on parallel benchmarks so should be used all the time.
 */
public final class SchedulerForkJoinPool extends SchedulerFromExecutor {

    public static final SchedulerForkJoinPool INSTANCE = new SchedulerForkJoinPool();

    private SchedulerForkJoinPool() {
        super(new SplitResponsibilityScheduledExecutorService(ForkJoinPool.commonPool(),
                Executors.newSingleThreadScheduledExecutor()));
    }
}
