package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
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

    @Test
    public void testSchedule() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        SchedulerComputationSticky s = new SchedulerComputationSticky();
        s.schedule(() -> latch.countDown());
        s.schedule(() -> latch.countDown());
        latch.await(5, TimeUnit.SECONDS);
        s.shutdown();
    }

    @Test
    public void testSchedulewithDelay() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        SchedulerComputationSticky s = new SchedulerComputationSticky();
        s.schedule(() -> latch.countDown(), 1, TimeUnit.MILLISECONDS);
        s.schedule(() -> latch.countDown(), 1, TimeUnit.MILLISECONDS);
        latch.await(5, TimeUnit.SECONDS);
        s.shutdown();
    }
    
    @Test
    public void testSchedulewithneNegativeDelay() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        SchedulerComputationSticky s = new SchedulerComputationSticky();
        s.schedule(() -> latch.countDown(), -1, TimeUnit.MILLISECONDS);
        s.schedule(() -> latch.countDown(), -1, TimeUnit.MILLISECONDS);
        latch.await(5, TimeUnit.SECONDS);
        s.shutdown();
    }

    @Test
    public void testSchedulePeriodically() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        SchedulerComputationSticky s = new SchedulerComputationSticky();
        Disposable a = s.schedulePeriodically(() -> latch.countDown(), 1, 1, TimeUnit.MILLISECONDS);
        Disposable b = s.schedulePeriodically(() -> latch.countDown(), 1, 1, TimeUnit.MILLISECONDS);
        latch.await(5, TimeUnit.SECONDS);
        a.dispose();
        b.dispose();
        s.shutdown();
    }
}
