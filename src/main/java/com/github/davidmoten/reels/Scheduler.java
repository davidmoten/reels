package com.github.davidmoten.reels;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.github.davidmoten.reels.internal.Constants;
import com.github.davidmoten.reels.internal.scheduler.SchedulerComputationSticky;
import com.github.davidmoten.reels.internal.scheduler.SchedulerDoNothing;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;
import com.github.davidmoten.reels.internal.scheduler.SchedulerFromExecutor;
import com.github.davidmoten.reels.internal.scheduler.SchedulerImmediate;
import com.github.davidmoten.reels.internal.scheduler.SchedulerIo;
import com.github.davidmoten.reels.internal.scheduler.TestScheduler;

public interface Scheduler extends CanSchedule {

    Worker createWorker();

    void shutdown();

    boolean requiresSerialization();

    static Scheduler defaultScheduler() {
        return forkJoin();
    }

    /**
     * Work-stealing Scheduler using the ForkJoin.common pool
     * @return Scheduler using the ForkJoin.common pool
     */
    static Scheduler forkJoin() {
        return SchedulerForkJoinPool.INSTANCE;
    }

    static Scheduler computation() {
        return forkJoin();
    }
    
    static Scheduler computationSticky() {
        // outperforms NonSticky
        return SchedulerComputationSticky.INSTANCE;
    }

    static Scheduler io() {
        return SchedulerIo.INSTANCE;
    }

    static Scheduler immediate() {
        return SchedulerImmediate.INSTANCE;
    }

    static Scheduler single() {
        return Constants.SINGLE;
    }
    
    static Scheduler newSingle() {
        return Scheduler.fromExecutor(Executors.newSingleThreadScheduledExecutor(Constants.NEW_SINGLE_THREAD_FACTORY), false);
    }

    static Scheduler fromExecutor(ScheduledExecutorService executor) {
        return new SchedulerFromExecutor(executor, true);
    }

    static Scheduler fromExecutor(ScheduledExecutorService executor, boolean requiresSerialization) {
        return new SchedulerFromExecutor(executor, requiresSerialization);
    }

    static Scheduler doNothing() {
        return SchedulerDoNothing.INSTANCE;
    }

    static TestScheduler test() {
        return new TestScheduler();
    }

}
