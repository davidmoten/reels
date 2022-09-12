package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.SchedulerComputation;
import com.github.davidmoten.reels.internal.SchedulerImmediate;

public interface Scheduler {

    static Scheduler computation() {
        return SchedulerComputation.INSTANCE;
    }

    static Scheduler io() {
        throw new UnsupportedOperationException();
    }

    static Scheduler newSingleThread() {
        throw new UnsupportedOperationException();
    }

    static Scheduler immediate() {
        return SchedulerImmediate.INSTANCE;
    }

    Worker createWorker();
    
    void shutdown();

}
