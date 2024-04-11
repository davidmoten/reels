package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MockedScheduledFuture<T> implements ScheduledFuture<T> {
    
    public static final Object ANY = new Object();
    
    private final List<String> events = new CopyOnWriteArrayList<>();
    private final List<Object> args = new CopyOnWriteArrayList<>();

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

    @Override
    public long getDelay(TimeUnit unit) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int compareTo(Delayed o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isCancelled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDone() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        // TODO Auto-generated method stub
        return null;
    }

}
