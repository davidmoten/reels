package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.mockito.Mockito;

public class ExecutorWorkerTest {

    @Test
    public void testDisposal() {
        ScheduledExecutorService s = Mockito.mock(ScheduledExecutorService.class);
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

    @SuppressWarnings("rawtypes")
    @Test
    public void testSchedule() {
        ScheduledExecutorService s = Mockito.mock(ScheduledExecutorService.class);
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        Future future = Mockito.mock(Future.class);
        Mockito.<Future>when(s.submit(Mockito.<Runnable>any())).thenReturn(future);
        ExecutorWorker w = new ExecutorWorker(s);
        assertTrue(w.schedule(r) instanceof FutureTask);
        Mockito.verify(s, Mockito.times(1)).submit(Mockito.<Runnable>any());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testScheduleWithZeroDelay() {
        ScheduledExecutorService s = Mockito.mock(ScheduledExecutorService.class);
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        Future future = Mockito.mock(Future.class);
        Mockito.<Future>when(s.submit(Mockito.<Runnable>any())).thenReturn(future);
        ExecutorWorker w = new ExecutorWorker(s);
        assertTrue(w.schedule(r, 0, TimeUnit.SECONDS) instanceof FutureTask);
        Mockito.verify(s, Mockito.times(1)).submit(Mockito.<Runnable>any());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testScheduleWithDelay() {
        ScheduledExecutorService s = Mockito.mock(ScheduledExecutorService.class);
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        ScheduledFuture future = Mockito.mock(ScheduledFuture.class);
        Mockito.<ScheduledFuture>when(s.schedule(Mockito.<Runnable>any(), Mockito.eq(1L), Mockito.eq(TimeUnit.SECONDS)))
                .thenReturn(future);
        ExecutorWorker w = new ExecutorWorker(s);
        assertTrue(w.schedule(r, 1, TimeUnit.SECONDS) instanceof FutureTask);
        Mockito.verify(s, Mockito.times(1)).schedule(Mockito.<Runnable>any(), Mockito.eq(1L), Mockito.eq(TimeUnit.SECONDS));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testSchedulePeriodically() {
        ScheduledExecutorService s = Mockito.mock(ScheduledExecutorService.class);
        AtomicBoolean b = new AtomicBoolean();
        Runnable r = () -> b.set(true);
        ScheduledFuture future = Mockito.mock(ScheduledFuture.class);
        Mockito.<ScheduledFuture>when(s.scheduleAtFixedRate(Mockito.<Runnable>any(), Mockito.eq(1L), Mockito.eq(1L),
                Mockito.eq(TimeUnit.SECONDS))).thenReturn(future);
        ExecutorWorker w = new ExecutorWorker(s);
        assertTrue(w.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS) instanceof FutureTask);
        Mockito.verify(s, Mockito.times(1)).scheduleAtFixedRate(Mockito.<Runnable>any(), Mockito.eq(1L), Mockito.eq(1L),
                Mockito.eq(TimeUnit.SECONDS));
    }

}
