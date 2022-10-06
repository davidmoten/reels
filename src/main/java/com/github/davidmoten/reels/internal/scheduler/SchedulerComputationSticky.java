package com.github.davidmoten.reels.internal.scheduler;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public final class SchedulerComputationSticky extends AbstractCanScheduleDisposable implements Scheduler {

    public static final SchedulerComputationSticky INSTANCE = new SchedulerComputationSticky();

    private final List<Worker> workers;

    private int index;

    // VisibleForTesting
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
    public Disposable _schedule(Runnable run) {
        return workers.get(nextIndex()).schedule(run);
    }

    @Override
    public Disposable _schedule(Runnable run, long delay, TimeUnit unit) {
        return workers.get(nextIndex()).schedule(run, delay, unit);
    }

    @Override
    public Disposable _schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
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

    @Override
    public void dispose() {
        shutdown();
    }

    @Override
    public boolean isDisposed() {
        return workers.isEmpty();
    }

}
