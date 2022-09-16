package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.Executors;

/**
 * This class present for benchmark purposes. SchedulerComputation is more thatn
 * twice as fast on parallel benchmarks so should be used all the time.
 */
public final class SchedulerComputationNonSticky extends SchedulerFromExecutor {

    public static final SchedulerComputationNonSticky INSTANCE = new SchedulerComputationNonSticky();

    private SchedulerComputationNonSticky() {
        super(Executors.newScheduledThreadPool(size(),
                SchedulerHelper.createThreadFactory("ReelsComputationNonSticky")));
    }

    private static int size() {
        return Integer.getInteger("reels.computation.pool.size", Runtime.getRuntime().availableProcessors());
    }

}
