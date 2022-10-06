package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;

public final class SchedulerForkJoinPool extends SchedulerFromExecutor {

    public static final SchedulerForkJoinPool INSTANCE = new SchedulerForkJoinPool();

    private SchedulerForkJoinPool() {
        super(new SplitResponsibilityScheduledExecutorService(ForkJoinPool.commonPool(),
                Executors.newSingleThreadScheduledExecutor()), true);
    }
}
