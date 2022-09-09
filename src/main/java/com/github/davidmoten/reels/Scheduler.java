package com.github.davidmoten.reels;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.internal.SchedulerComputation;
import com.github.davidmoten.reels.internal.SchedulerFromExecutor;
import com.github.davidmoten.reels.internal.SchedulerIo;

public interface Scheduler {

    static Scheduler computation() {
        return SchedulerComputation.INSTANCE;
    }
    
    static Scheduler io() {
        return SchedulerIo.INSTANCE;
    }

    static Scheduler from(ScheduledExecutorService executor) {
        return new SchedulerFromExecutor(executor);
    }

    Disposable schedule(Runnable run);

    Disposable schedule(Runnable run, long delay, TimeUnit unit);

    Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit);
}
