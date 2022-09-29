package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public class SchedulerIoTest {

    @Test
    public void test() throws InterruptedException {
        Worker w = Scheduler.io().createWorker();
        CountDownLatch latch = new CountDownLatch(1);
        w.schedule(() -> latch.countDown(), 10, TimeUnit.MILLISECONDS);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        w.dispose();
        assertTrue(w.isDisposed());
        w.dispose();
        assertTrue(w.isDisposed());
    }

}
