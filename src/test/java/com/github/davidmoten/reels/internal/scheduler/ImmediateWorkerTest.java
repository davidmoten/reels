package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class ImmediateWorkerTest {

    @Test
    public void testScheduleAfterDisposeDoesNothing() {
        AtomicBoolean b = new AtomicBoolean();
        ImmediateWorker w = new ImmediateWorker();
        assertFalse(w.isDisposed());
        w.dispose();
        assertTrue(w.isDisposed());
        w.schedule(() -> b.set(true));
        assertFalse(b.get());
    }

    @Test
    public void testScheduleAfterDelay() {
        AtomicBoolean b = new AtomicBoolean();
        ImmediateWorker w = new ImmediateWorker();
        w.schedule(() -> b.set(true), 1, TimeUnit.MILLISECONDS);
        assertTrue(b.get());
    }

    @Test
    public void testScheduleAfterDelayDisposedDoesNothing() {
        AtomicBoolean b = new AtomicBoolean();
        ImmediateWorker w = new ImmediateWorker();
        assertFalse(w.isDisposed());
        w.dispose();
        assertTrue(w.isDisposed());
        w.schedule(() -> b.set(true), 1, TimeUnit.MILLISECONDS);
        assertFalse(b.get());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSchedulePeriodically() {
        ImmediateWorker w = new ImmediateWorker();
        w.schedulePeriodically(() -> {
        }, 1, 1, TimeUnit.MILLISECONDS);
    }

}
