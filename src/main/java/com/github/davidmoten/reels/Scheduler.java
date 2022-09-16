package com.github.davidmoten.reels;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.internal.scheduler.SchedulerComputationSticky;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;
import com.github.davidmoten.reels.internal.scheduler.SchedulerFromExecutor;
import com.github.davidmoten.reels.internal.scheduler.SchedulerImmediate;
import com.github.davidmoten.reels.internal.scheduler.SchedulerIo;

public interface Scheduler {

    Worker createWorker();
    
    Disposable schedule(Runnable run);

    Disposable schedule(Runnable run, long delay, TimeUnit unit);

    Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit);

    void shutdown();
    
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
    
    static Scheduler fromExecutor(ScheduledExecutorService executor) {
        return new SchedulerFromExecutor(executor);
    }

}
