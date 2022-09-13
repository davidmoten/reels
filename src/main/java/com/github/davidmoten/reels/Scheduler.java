package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.scheduler.SchedulerComputation;
import com.github.davidmoten.reels.internal.scheduler.SchedulerImmediate;
import com.github.davidmoten.reels.internal.scheduler.SchedulerIo;

public interface Scheduler {

    static Scheduler computation() {
        return SchedulerComputation.INSTANCE;
    }

    static Scheduler io() {
        return SchedulerIo.INSTANCE;
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
