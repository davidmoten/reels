package com.github.davidmoten.reels;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.internal.SchedulerComputation;
import com.github.davidmoten.reels.internal.SchedulerFromExecutor;
import com.github.davidmoten.reels.internal.SchedulerImmediate;
import com.github.davidmoten.reels.internal.SchedulerIo;
import com.github.davidmoten.reels.internal.SchedulerSingleThread;

public interface Scheduler {
    
    static Scheduler computation() {
        return SchedulerComputation.INSTANCE;
    }
    
    static Scheduler io() {
        return SchedulerIo.INSTANCE;
    }
    
    static Scheduler newSingleThread() {
        return new SchedulerSingleThread();
    }
    
    static Scheduler immediate() {
        return SchedulerImmediate.INSTANCE;
    }

    static Scheduler from(ExecutorService direct, ScheduledExecutorService scheduled) {
        return new SchedulerFromExecutor(direct, scheduled);
    }

    Disposable schedule(Runnable run);

    Disposable schedule(Runnable run, long delay, TimeUnit unit);

    Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit);
}
