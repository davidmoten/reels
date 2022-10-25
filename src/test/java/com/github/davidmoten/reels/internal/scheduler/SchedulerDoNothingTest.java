package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.reels.Scheduler;

public class SchedulerDoNothingTest {

    @Test
    public void test() {
        Scheduler s = Scheduler.doNothing();
        assertTrue(s.createWorker().isDisposed());
        assertFalse(s.requiresDrainSynchronization());
        AtomicInteger c = new AtomicInteger();
        Runnable r = () -> c.incrementAndGet();
        assertTrue(s.schedule(r).isDisposed());
        assertEquals(0, c.get());
        assertTrue(s.schedule(r, 1, TimeUnit.MILLISECONDS).isDisposed());
        assertEquals(0, c.get());
        assertTrue(s.schedulePeriodically(r, 5, 1, TimeUnit.MILLISECONDS).isDisposed());
    }

}
