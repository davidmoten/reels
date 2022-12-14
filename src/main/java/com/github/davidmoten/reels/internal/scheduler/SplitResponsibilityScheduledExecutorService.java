package com.github.davidmoten.reels.internal.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class SplitResponsibilityScheduledExecutorService implements ScheduledExecutorService {

    private final ExecutorService executor;
    private final ScheduledExecutorService scheduled;

    public SplitResponsibilityScheduledExecutorService(ExecutorService executor, ScheduledExecutorService scheduled) {
        this.executor = executor;
        this.scheduled = scheduled;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return scheduled.schedule(command, delay, unit);
    }

    @Override
    public void execute(Runnable command) {
        executor.execute(command);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return scheduled.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduled.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        scheduled.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> list = new ArrayList<>(executor.shutdownNow());
        list.addAll(scheduled.shutdownNow());
        return list;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return scheduled.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown() && scheduled.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return executor.isTerminated() && scheduled.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit) && scheduled.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return scheduled.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return scheduled.invokeAny(tasks, timeout, unit);
    }

}
