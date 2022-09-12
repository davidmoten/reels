package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.SchedulerComputation;
import com.github.davidmoten.reels.internal.SchedulerImmediate;

public interface Scheduler {

    static Scheduler computation() {
        return SchedulerComputation.INSTANCE;
    }

    static Scheduler io() {
        return null;
    }

    static Scheduler newSingleThread() {
        return null;
    }

    static Scheduler immediate() {
        return SchedulerImmediate.INSTANCE;
    }

    Worker createWorker();

}
