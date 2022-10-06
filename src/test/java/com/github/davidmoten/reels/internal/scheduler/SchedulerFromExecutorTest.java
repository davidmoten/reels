package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.davidmoten.reels.Disposable;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SchedulerFromExecutorTest {

    @Test
    public void testScheduleDirect() {
        ScheduledExecutorService a = Mockito.mock(ScheduledExecutorService.class);
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        Runnable r = () -> {
        };
        b.schedule(r);
        verify(a, times(1)).submit(r);
        verifyNoMoreInteractions(a);
    }

    @Test
    public void testScheduleWithNegativeDelay() {
        ScheduledExecutorService a = Mockito.mock(ScheduledExecutorService.class);
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        Runnable r = () -> {
        };
        b.schedule(r, -1, TimeUnit.SECONDS);
        verify(a, times(1)).submit(r);
        verifyNoMoreInteractions(a);
    }

    @Test
    public void testScheduleWithDelay() {
        ScheduledExecutorService a = Mockito.mock(ScheduledExecutorService.class);
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        Runnable r = () -> {
        };
        b.schedule(r, 1, TimeUnit.SECONDS);
        verify(a, times(1)).schedule(eq(r), eq(1L), eq(TimeUnit.SECONDS));
        verifyNoMoreInteractions(a);
    }

    @Test
    public void testSchedulePeriodically() {
        ScheduledExecutorService a = Mockito.mock(ScheduledExecutorService.class);
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        Runnable r = () -> {
        };
        b.schedulePeriodically(r, 1, 2, TimeUnit.SECONDS);
        verify(a, times(1)).scheduleAtFixedRate(eq(r), eq(1L), eq(2L), eq(TimeUnit.SECONDS));
        verifyNoMoreInteractions(a);
    }

    @Test
    public void testShutdown() {
        ScheduledExecutorService a = Mockito.mock(ScheduledExecutorService.class);
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        b.shutdown();
        Runnable r = () -> {
        };
        assertTrue(Disposable.disposed() == b.schedule(r));
        verify(a, times(1)).shutdownNow();
        verifyNoMoreInteractions(a);
    }

}
