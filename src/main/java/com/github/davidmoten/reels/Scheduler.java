package com.github.davidmoten.reels;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public interface Scheduler {

    static Scheduler computation() {
        return SchedulerComputation.INSTANCE;
    }

    static Scheduler from(ScheduledExecutorService executor) {
        return new SchedulerFromExecutor(executor);
    }

    Disposable schedule(Runnable run);

    Disposable schedule(Runnable run, long delay, TimeUnit unit);

    Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit);
}
