package com.github.davidmoten.reels.internal.scheduler;

import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public class TestScheduler extends AtomicInteger implements Scheduler {

    private static final long serialVersionUID = 4484385678522955967L;
    private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<ScheduledTask>(
            (a, b) -> Long.compare(a.time, b.time));

    private long time = 0;
    private boolean running;
    private boolean disposed;

    @Override
    public Worker createWorker() {
        return new Worker() {

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
            public Disposable schedule(Runnable run) {
                if (disposed) {
                    return Disposable.disposed();
                }
                return TestScheduler.this.schedule(run);
            }

            @Override
            public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
                if (delay <= 0) {
                    return schedule(run);
                }
                if (disposed) {
                    return Disposable.disposed();
                }
                return TestScheduler.this.schedule(run, delay, unit);
            }

            @Override
            public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
                if (disposed) {
                    return Disposable.disposed();
                }
                return TestScheduler.this.schedulePeriodically(run, initialDelay, period, unit);
            }

        };
    }

    @Override
    public Disposable schedule(Runnable run) {
        if (disposed) {
            return Disposable.disposed();
        }
        ScheduledTask task = new ScheduledTask(time, run);
        queue.add(task);
        drain();
        return task.disposable;
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (disposed) {
            return Disposable.disposed();
        }
        ScheduledTask task = new ScheduledTask(time + unit.toMillis(delay), run);
        queue.add(task);
        drain();
        return task.disposable;
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        if (disposed) {
            return Disposable.disposed();
        }
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

}