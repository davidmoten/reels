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
        MockedScheduledExecutorService a = new MockedScheduledExecutorService();
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        Runnable r = () -> {
        };
        b.schedule(r);
        a.assertEvents("submit");
        a.assertArgs(r);
    }

    @Test
    public void testScheduleWithNegativeDelay() {
        MockedScheduledExecutorService a = new MockedScheduledExecutorService();
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        Runnable r = () -> {
        };
        b.schedule(r, -1, TimeUnit.SECONDS);
        a.assertEvents("submit");
        a.assertArgs(r);
    }

    @Test
    public void testScheduleWithDelay() {
        MockedScheduledExecutorService a = new MockedScheduledExecutorService();
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        Runnable r = () -> {
        };
        b.schedule(r, 1, TimeUnit.SECONDS);
        a.assertEvents("schedule");
        a.assertArgs(r, 1L, TimeUnit.SECONDS);
    }

    @Test
    public void testSchedulePeriodically() {
        MockedScheduledExecutorService a = new MockedScheduledExecutorService();
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        Runnable r = () -> {
        };
        b.schedulePeriodically(r, 1, 2, TimeUnit.SECONDS);
        a.assertEvents("scheduleAtFixedRate");
        a.assertArgs(r, 1L, 2L, TimeUnit.SECONDS);
    }

    @Test
    public void testShutdown() {
        MockedScheduledExecutorService a = new MockedScheduledExecutorService();
        SchedulerFromExecutor b = new SchedulerFromExecutor(a, false);
        b.shutdown();
        Runnable r = () -> {
        };
        assertTrue(Disposable.disposed() == b.schedule(r));
        a.assertEvents("shutdownNow");
    }

}
