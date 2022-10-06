package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

/**
 * When used as an Actor scheduler behaves like a trampoline scheduler. Does not
 * behave like a trampoline scheduler for other purposes!
 */
public class SchedulerImmediate implements Scheduler {

    public static final SchedulerImmediate INSTANCE = new SchedulerImmediate();

    private SchedulerImmediate() {
    }

    @Override
    public Worker createWorker() {
        return new ImmediateWorker();
    }

    @Override
    public void shutdown() {
        // no disposing required
    }

    @Override
    public Disposable schedule(Runnable run) {
        run.run();
        return Disposable.disposed();
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        try {
            unit.sleep(delay);
            return schedule(run);
        } catch (InterruptedException e) {
            return Disposable.disposed();
        }
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        throw new UnsupportedOperationException("immediate scheduler does not support periodic scheduling");
    }

    @Override
    public boolean requiresSerialization() {
        return false;
    }

}