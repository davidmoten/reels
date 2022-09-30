package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

public class WorkerDisposedTest {

    @Test
    public void test() {
        WorkerDisposed w = WorkerDisposed.INSTANCE;
        assertTrue(w.isDisposed());
        w.dispose();
        assertTrue(w.isDisposed());
        AtomicBoolean b = new AtomicBoolean();
        assertTrue(w.schedule(() -> b.set(true)).isDisposed());
        assertFalse(b.get());
        assertTrue(w.schedule(() -> b.set(true), 1, TimeUnit.MILLISECONDS).isDisposed());
        assertTrue(w.schedulePeriodically(() -> b.set(true), 1,1,  TimeUnit.MILLISECONDS).isDisposed());
    }

}
