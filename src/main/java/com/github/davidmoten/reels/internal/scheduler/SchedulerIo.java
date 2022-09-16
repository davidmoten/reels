/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.CompositeDisposable;

/**
 * Scheduler that creates and caches a set of thread pools and reuses them if
 * possible.
 */
public final class SchedulerIo implements Scheduler {

    private static final String WORKER_THREAD_NAME_PREFIX = "ReelsCachedThreadScheduler";
    private static final String EVICTOR_THREAD_NAME_PREFIX = "ReelsCachedWorkerPoolEvictor";
    private static final ThreadFactory WORKER_THREAD_FACTORY = SchedulerHelper.createThreadFactory(WORKER_THREAD_NAME_PREFIX);
    private static final ThreadFactory EVICTOR_THREAD_FACTORY = SchedulerHelper.createThreadFactory(EVICTOR_THREAD_NAME_PREFIX);

    /**
     * The name of the system property for setting the keep-alive time (in seconds)
     * for this Scheduler workers.
     */
    private static final String KEY_KEEP_ALIVE_TIME = "reels.io-keep-alive-time.seconds";
    public static final long KEEP_ALIVE_TIME_DEFAULT = 60;
    private static final TimeUnit KEEP_ALIVE_UNIT = TimeUnit.SECONDS;
    private static final long KEEP_ALIVE_TIME = Long.getLong(KEY_KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_DEFAULT);
    private static final ThreadWorker SHUTDOWN_THREAD_WORKER = createShutdownWorker();
    private final ThreadFactory threadFactory;
    private final AtomicReference<CachedWorkerPool> pool;
    private static final CachedWorkerPool NONE = createEmptyCachedWorkerPool();

    public static final SchedulerIo INSTANCE = new SchedulerIo();

    private SchedulerIo() {
        this(WORKER_THREAD_FACTORY);
    }

    /**
     * Constructs an IoScheduler with the given thread factory and starts the pool
     * of workers.
     * 
     * @param threadFactory thread factory to use for creating worker threads. Note
     *                      that this takes precedence over any system properties
     *                      for configuring new thread creation. Cannot be null.
     */
    private SchedulerIo(ThreadFactory threadFactory) {
        Preconditions.checkNotNull(threadFactory);
        this.threadFactory = threadFactory;
        this.pool = new AtomicReference<>(NONE);
        start();
    }

    static final class CachedWorkerPool implements Runnable {
        private final long keepAliveTime;
        private final ConcurrentLinkedQueue<ThreadWorker> expiringWorkerQueue;
        private final CompositeDisposable allWorkers;
        private final ScheduledExecutorService evictorService;
        private final Future<?> evictorTask;
        private final ThreadFactory threadFactory;

        CachedWorkerPool(long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
            this.keepAliveTime = unit != null ? unit.toNanos(keepAliveTime) : 0L;
            this.expiringWorkerQueue = new ConcurrentLinkedQueue<>();
            this.allWorkers = new CompositeDisposable();
            this.threadFactory = threadFactory;

            ScheduledExecutorService evictor = null;
            Future<?> task = null;
            if (unit != null) {
                evictor = Executors.newScheduledThreadPool(1, EVICTOR_THREAD_FACTORY);
                task = evictor.scheduleWithFixedDelay(this, this.keepAliveTime, this.keepAliveTime,
                        TimeUnit.NANOSECONDS);
            }
            evictorService = evictor;
            evictorTask = task;
        }

        @Override
        public void run() {
            evictExpiredWorkers(expiringWorkerQueue, allWorkers);
        }

        ThreadWorker get() {
            if (allWorkers.isDisposed()) {
                return SHUTDOWN_THREAD_WORKER;
            }
            while (!expiringWorkerQueue.isEmpty()) {
                ThreadWorker threadWorker = expiringWorkerQueue.poll();
                if (threadWorker != null) {
                    return threadWorker;
                }
            }

            // No cached worker found, so create a new one.
            ThreadWorker w = new ThreadWorker(threadFactory);
            allWorkers.add(w);
            return w;
        }

        void release(ThreadWorker threadWorker) {
            // Refresh expire time before putting worker back in pool
            threadWorker.setExpirationTime(now() + keepAliveTime);
            expiringWorkerQueue.offer(threadWorker);
        }

        static void evictExpiredWorkers(ConcurrentLinkedQueue<ThreadWorker> expiringWorkerQueue,
                CompositeDisposable allWorkers) {
            if (!expiringWorkerQueue.isEmpty()) {
                long currentTimestamp = now();

                for (ThreadWorker threadWorker : expiringWorkerQueue) {
                    if (threadWorker.getExpirationTime() <= currentTimestamp) {
                        if (expiringWorkerQueue.remove(threadWorker)) {
                            allWorkers.remove(threadWorker);
                        }
                    } else {
                        // Queue is ordered with the worker that will expire first in the beginning, so
                        // when we find a non-expired worker we can stop evicting.
                        break;
                    }
                }
            }
        }

        static long now() {
            return System.nanoTime();
        }

        void shutdown() {
            allWorkers.dispose();
            if (evictorTask != null) {
                evictorTask.cancel(true);
            }
            if (evictorService != null) {
                evictorService.shutdownNow();
            }
        }
    }

    private void start() {
        CachedWorkerPool update = new CachedWorkerPool(KEEP_ALIVE_TIME, KEEP_ALIVE_UNIT, threadFactory);
        if (!pool.compareAndSet(NONE, update)) {
            update.shutdown();
        }
    }

    @Override
    public void shutdown() {
        CachedWorkerPool curr = pool.getAndSet(NONE);
        if (curr != NONE) {
            curr.shutdown();
        }
    }

    @Override
    public Worker createWorker() {
        return new EventLoopWorker(pool.get());
    }

    public int size() {
        return pool.get().allWorkers.size();
    }

    static final class EventLoopWorker implements Worker {
        private final CompositeDisposable tasks;
        private final CachedWorkerPool pool;
        private final ThreadWorker threadWorker;
        private final AtomicBoolean once = new AtomicBoolean();

        EventLoopWorker(CachedWorkerPool pool) {
            this.pool = pool;
            this.tasks = new CompositeDisposable();
            this.threadWorker = pool.get();
        }

        @Override
        public void dispose() {
            if (once.compareAndSet(false, true)) {
                tasks.dispose();

                // releasing the pool should be the last action
                pool.release(threadWorker);
            }
        }

        @Override
        public boolean isDisposed() {
            return once.get();
        }

        @Override
        public Disposable schedule(Runnable action, long delayTime, TimeUnit unit) {
            if (tasks.isDisposed()) {
                // don't schedule, we are unsubscribed
                return Disposable.disposed();
            }
            Disposable d = threadWorker.schedule(action, delayTime, unit);
            tasks.add(d);
            return d;
        }

        @Override
        public Disposable schedule(Runnable run) {
            Disposable d = threadWorker.schedule(run);
            tasks.add(d);
            return d;
        }

        @Override
        public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
            Disposable d = threadWorker.schedulePeriodically(run, initialDelay, period, unit);
            tasks.add(d);
            return d;
        }
    }

    static final class ThreadWorker extends NewThreadWorker {

        long expirationTime;

        ThreadWorker(ThreadFactory threadFactory) {
            super(threadFactory);
            this.expirationTime = 0L;
        }

        public long getExpirationTime() {
            return expirationTime;
        }

        public void setExpirationTime(long expirationTime) {
            this.expirationTime = expirationTime;
        }
    }

    private static ThreadWorker createShutdownWorker() {
        ThreadWorker w = new ThreadWorker(SchedulerHelper.createThreadFactory("ReelsCachedThreadSchedulerShutdown"));
        w.dispose();
        return w;
    }

    private static CachedWorkerPool createEmptyCachedWorkerPool() {
        CachedWorkerPool p = new CachedWorkerPool(0, null, WORKER_THREAD_FACTORY);
        p.shutdown();
        return p;
    }

    @Override
    public Disposable schedule(Runnable run) {
        Worker w = createWorker();
        Disposable d = w.schedule(run);
        return new CompositeDisposable(d, w);
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        Worker w = createWorker();
        Disposable d = w.schedule(run, delay, unit);
        return new CompositeDisposable(d, w);
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        Worker w = createWorker();
        Disposable d = w.schedulePeriodically(run, initialDelay, period, unit);
        return new CompositeDisposable(d, w);
    }

}
