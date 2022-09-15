package com.github.davidmoten.reels.internal.scheduler;

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

}