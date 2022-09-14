package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.Util;

public class SchedulerComputationNonSticky implements Scheduler {

    private final ScheduledExecutorService executor;
    private final NonStickyWorker worker;

    private SchedulerComputationNonSticky() {
        executor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(),
                Util.createThreadFactory("ReelsComputationNonSticky"));
        worker = new NonStickyWorker(executor);
    }

    @Override
    public Worker createWorker() {
        return new SchedulerWorker(worker);
    }

    @Override
    public void shutdown() {
        worker.dispose();
        executor.shutdownNow();
    }

    private static final class NonStickyWorker implements Worker {
        private volatile boolean disposed;
        private final ScheduledExecutorService executor;

        NonStickyWorker(ScheduledExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public Disposable schedule(Runnable run) {
            if (disposed) {
                return Disposable.DISPOSED;
            } else {
                return new FutureTask(executor.submit(run));
            }
        }

        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            if (disposed) {
                return Disposable.DISPOSED;
            } else {
                return new FutureTask(executor.schedule(run, delay, unit));
            }
        }

        @Override
        public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
            if (disposed) {
                return Disposable.DISPOSED;
            } else {
                return new FutureTask(executor.scheduleAtFixedRate(run, initialDelay, period, unit));
            }
        }
    }

}
