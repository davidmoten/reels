package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.scheduler.SchedulerComputationSticky;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;
import com.github.davidmoten.reels.internal.scheduler.SchedulerImmediate;
import com.github.davidmoten.reels.internal.scheduler.SchedulerIo;

public interface Scheduler {

    static Scheduler forkJoin() {
        return SchedulerForkJoinPool.INSTANCE;
    }

    static Scheduler computation() {
        // outperforms NonSticky
        return SchedulerComputationSticky.INSTANCE;
    }

    static Scheduler io() {
        return SchedulerIo.INSTANCE;
    }

    static Scheduler immediate() {
        return SchedulerImmediate.INSTANCE;
    }

    Worker createWorker();

    void shutdown();

}
