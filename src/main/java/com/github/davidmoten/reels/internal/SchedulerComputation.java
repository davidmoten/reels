package com.github.davidmoten.reels.internal;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public final class SchedulerComputation extends AtomicInteger implements Scheduler {

    private static final long serialVersionUID = -6316108674515948678L;

    public static final Scheduler INSTANCE = new SchedulerComputation();

    private final List<Worker> workers;

    private int index;

    private SchedulerComputation() {
        int size = Util.systemPropertyInt("reels.computation.pool.size", Runtime.getRuntime().availableProcessors());
        ThreadFactory factory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                String name = "ReelsComputation-" + incrementAndGet();
                Thread t = new Thread(r, name);
                t.setPriority(Thread.NORM_PRIORITY);
                t.setDaemon(true);
                return t;
            }
        };
        workers = IntStream //
                .range(0, size) //
                .mapToObj(n -> new NewThreadWorker(factory)) //
                .collect(Collectors.toList());
    }

    @Override
    public Worker createWorker() {
        return new SchedulerWorker(workers.get(index++ % workers.size()));
    }

    // TODO shutdown

}
