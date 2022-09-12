package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

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