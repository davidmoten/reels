package com.github.davidmoten.reels.internal.scheduler;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public class TestScheduler extends AbstractCanScheduleDisposable implements Scheduler {

    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<ScheduledTask>(
            (a, b) -> Long.compare(a.time, b.time));

    private long time = 0;
    private boolean running;
    private boolean disposed;

    @Override
    public Worker createWorker() {
        return new TestWorker();
    }

    private final class TestWorker extends AbstractCanScheduleDisposable implements Worker {

        volatile boolean disposed;

        @Override
        public void dispose() {
            disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return disposed;
        }

        @Override
        protected Disposable _schedule(Runnable run) {
            return TestScheduler.this.schedule(run);
        }

        @Override
        protected Disposable _schedule(Runnable run, long delay, TimeUnit unit) {
            return TestScheduler.this.schedule(run, delay, unit);
        }

        @Override
        protected Disposable _schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
            return TestScheduler.this.schedulePeriodically(run, initialDelay, period, unit);
        }

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
    protected Disposable _schedule(Runnable run) {
        ScheduledTask task = new ScheduledTask(time, run);
        queue.add(task);
        drain();
        return task.disposable;
    }

    @Override
    protected Disposable _schedule(Runnable run, long delay, TimeUnit unit) {
        ScheduledTask task = new ScheduledTask(time + unit.toMillis(delay), run);
        queue.add(task);
        drain();
        return task.disposable;

    }

    @Override
    protected Disposable _schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        Disposable d = Disposable.simple();
        ScheduledTask task = new ScheduledTask(time + unit.toMillis(initialDelay), run, unit.toMillis(period), d);
        queue.add(task);
        drain();
        return d;
    }

    private void drain() {
        if (running) {
            return;
        }
        running = true;
        while (true) {
            ScheduledTask task = queue.peek();
            if (task == null) {
                break;
            }
            if (task.time <= time) {
                queue.poll();
                if (!task.disposable.isDisposed()) {
                    task.run.run();
                }
                if (task.intervalMs > 0 && !task.disposable.isDisposed()) {
                    queue.add(
                            new ScheduledTask(task.time + task.intervalMs, task.run, task.intervalMs, task.disposable));
                }
            } else {
                break;
            }
        }
        running = false;
    }

    @Override
    public void shutdown() {
        queue.clear();
        disposed = true;
    }

    public TestScheduler advance(long duration, TimeUnit unit) {
        time += unit.toMillis(duration);
        drain();
        return this;
    }

    private static final class ScheduledTask {
        final long time;
        final Runnable run;
        final long intervalMs;
        final Disposable disposable;

        ScheduledTask(long time, Runnable run) {
            this(time, run, 0, Disposable.simple());
        }

        ScheduledTask(long time, Runnable run, long intervalMs, Disposable disposable) {
            this.time = time;
            this.run = run;
            this.intervalMs = intervalMs;
            this.disposable = disposable;
        }

    }

    @Override
    public boolean requiresSynchronization() {
        return false;
    }

}