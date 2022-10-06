package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.davidmoten.reels.Disposable;

public class SchedulerComputationStickyTest {

    @Test
    public void testShutdownt() {
        SchedulerComputationSticky s = new SchedulerComputationSticky();
        s.shutdown();
        assertTrue(Disposable.disposed() == s.schedule(() -> {
        }));
        assertTrue(Disposable.disposed() == s.schedule(() -> {
        }, 1, TimeUnit.SECONDS));
        assertTrue(Disposable.disposed() == s.schedulePeriodically(() -> {
        }, 1, 2, TimeUnit.SECONDS));
    }

}
