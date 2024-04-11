package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockedScheduledExecutorService implements ScheduledExecutorService {
    
    public static final Object ANY = new Object();
    
    private final List<String> events = new CopyOnWriteArrayList<>();
    private final List<Object> args = new CopyOnWriteArrayList<>();
    private ScheduledFuture<?> future;

    public List<String> events() {
        return events;
    }
    
    public List<Object> args() {
        return args;
    }
    
    public void assertEvents(String... events) {
        assertEquals(Arrays.asList(events), this.events);
    }
    
    public void assertArgs(Object... args) {
        assertEquals(this.args.size(), args.length);
        for (int i = 0; i < this.args.size(); i++) {
            if (args[i] != ANY ) {
                assertEquals(this.args.get(i), args[i]);
            }
        }
    }
    
    private void addEvent(Object... args) {
        StackTraceElement[] a = Thread.currentThread().getStackTrace();
        StackTraceElement previous = a[2];
        events.add(previous.getMethodName());
        for (Object o: args) {
            this.args.add(o);
        }
    }
    
    public void returnThisFuture(ScheduledFuture<?> future) {
        this.future = future;
    }
    
    @Override
    public void shutdown() {
        addEvent();
    }
    
    @Override
    public List<Runnable> shutdownNow() {
        addEvent();
        return null;
    }

    @Override
    public boolean isShutdown() {
        addEvent();
        return false;
    }

    @Override
    public boolean isTerminated() {
        addEvent();
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        addEvent(timeout, unit);
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        addEvent(task);
        return (Future<T>) future;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        addEvent(task, result);
        return (Future<T>) future;
    }

    @Override
    public Future<?> submit(Runnable task) {
        addEvent(task);
        return future;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        addEvent(tasks);
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        addEvent(tasks, timeout, unit);
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        addEvent(tasks);
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        addEvent(tasks, timeout, unit);
        return null;
    }

    @Override
    public void execute(Runnable command) {
        addEvent(command);        
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        addEvent(command, delay, unit);
        return future;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        addEvent(callable, delay, unit);
        return (ScheduledFuture<V>) future;
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        addEvent(command, initialDelay, period, unit);
        return future;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        addEvent(command, initialDelay, delay, unit);
        return future;
    }

}
