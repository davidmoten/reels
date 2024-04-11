package com.github.davidmoten.reels.internal.scheduler;

import static com.github.davidmoten.reels.internal.scheduler.MockedScheduledExecutorService.ANY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.davidmoten.reels.Disposable;

public class ExecutorWorkerTest {

    @Test
    public void testDisposal() {
        MockedScheduledExecutorService s = new MockedScheduledExecutorService();
        ExecutorWorker w = new ExecutorWorker(s);
        assertFalse(w.isDisposed());
        w.dispose();
        assertTrue(w.isDisposed());
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        assertTrue(w.schedule(r).isDisposed());
        assertFalse(b.get());
        assertTrue(w.schedule(r, 1, TimeUnit.MILLISECONDS).isDisposed());
        assertFalse(b.get());
        assertTrue(w.schedulePeriodically(r, 1, 1, TimeUnit.MILLISECONDS).isDisposed());
        assertFalse(b.get());
    }

    @Test
    public void testSchedule() {
        MockedScheduledExecutorService s = new MockedScheduledExecutorService();
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        ScheduledFuture<?> future = new MockedScheduledFuture<>();
        s.returnThisFuture(future);
        ExecutorWorker w = new ExecutorWorker(s);
        Disposable d = w.schedule(r);
        assertTrue(d instanceof FutureTask);
        assertFalse(d.isDisposed());
        s.assertEvents("submit");
        s.assertArgs(r);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testScheduleWithZeroDelay() {
        MockedScheduledExecutorService s = new MockedScheduledExecutorService();
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        ScheduledFuture future = new MockedScheduledFuture<>();
        s.returnThisFuture(future);
        ExecutorWorker w = new ExecutorWorker(s);
        assertTrue(w.schedule(r, 0, TimeUnit.SECONDS) instanceof FutureTask);
        s.assertEvents("submit");
        s.assertArgs(r);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testScheduleWithDelay() {
        MockedScheduledExecutorService s = new MockedScheduledExecutorService();
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        ScheduledFuture future = new MockedScheduledFuture<>();
        s.returnThisFuture(future);
        ExecutorWorker w = new ExecutorWorker(s);
        assertTrue(w.schedule(r, 1, TimeUnit.SECONDS) instanceof FutureTask);
        s.assertEvents("schedule");
        s.assertArgs(r, 1L, TimeUnit.SECONDS);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testSchedulePeriodically() {
        MockedScheduledExecutorService s = new MockedScheduledExecutorService();
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        ScheduledFuture future = new MockedScheduledFuture<>();
        s.returnThisFuture(future);
        ExecutorWorker w = new ExecutorWorker(s);
        assertTrue(w.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS) instanceof FutureTask);
        s.assertEvents("scheduleAtFixedRate");
        s.assertArgs(ANY, 1L, 1L, TimeUnit.SECONDS);
    }

}
