package com.github.davidmoten.reels.internal.scheduler;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public final class SchedulerComputationSticky extends AtomicInteger implements Scheduler {

    private static final long serialVersionUID = -6316108674515948678L;

    public static final SchedulerComputationSticky INSTANCE = new SchedulerComputationSticky();

    private final List<Worker> workers;

    private int index;

    //VisibleForTesting
    SchedulerComputationSticky() {
        int size = Integer.getInteger("reels.computation.pool.size", Runtime.getRuntime().availableProcessors());
        ThreadFactory factory = SchedulerHelper.createThreadFactory("ReelsComputation");
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

    @Override
    public Disposable schedule(Runnable run) {
        if (workers.isEmpty()) {
            return Disposable.disposed();
        }
        return workers.get(nextIndex()).schedule(run);
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (delay <= 0) {
            return schedule(run);
        } else {
            if (workers.isEmpty()) {
                return Disposable.disposed();
            }
            return workers.get(nextIndex()).schedule(run, delay, unit);
        }
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        if (workers.isEmpty()) {
            return Disposable.disposed();
        }
        return workers.get(nextIndex()).schedulePeriodically(run, initialDelay, period, unit);
    }

    private int nextIndex() {
        index = (index + 1) % workers.size();
        return index;
    }

    @Override
    public boolean requiresSerialization() {
        return false;
    }
    
}
