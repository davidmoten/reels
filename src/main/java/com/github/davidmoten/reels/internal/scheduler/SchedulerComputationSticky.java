package com.github.davidmoten.reels.internal.scheduler;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.Util;

public final class SchedulerComputationSticky extends AtomicInteger implements Scheduler {

    private static final long serialVersionUID = -6316108674515948678L;

    public static final SchedulerComputationSticky INSTANCE = new SchedulerComputationSticky();

    private final List<Worker> workers;

    private int index;

    private SchedulerComputationSticky() {
        int size = Integer.getInteger("reels.computation.pool.size", Runtime.getRuntime().availableProcessors());
        ThreadFactory factory = Util.createThreadFactory("ReelsComputation");
        workers = IntStream //
                .range(0, size) //
                .mapToObj(n -> new NewThreadWorker(factory)) //
                .collect(Collectors.toList());
    }

    @Override
    public Worker createWorker() {
        return new SchedulerWorker(workers.get(index++ % workers.size()));
    }

    @Override
    public void shutdown() {
        for (Worker w : workers) {
            w.dispose();
        }
        workers.clear();
    }

}