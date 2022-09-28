package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Worker;

public class TestSchedulerTest {

    @Test
    public void test() {
        TestScheduler ts = Scheduler.test();
        AtomicInteger a = new AtomicInteger();
        Worker w = ts.createWorker();
        w.schedule(() -> a.set(1));
        assertEquals(1, a.get());

        // schedule with delay
        w.schedule(() -> a.set(2), 1, TimeUnit.SECONDS);
        assertEquals(1, a.get());
        ts.advance(500, TimeUnit.MILLISECONDS);
        assertEquals(1, a.get());
        ts.advance(500, TimeUnit.MILLISECONDS);
        assertEquals(2, a.get());

        // schedule with delay, advanced over
        w.schedule(() -> a.set(3), 1, TimeUnit.SECONDS);
        assertEquals(2, a.get());
        ts.advance(500, TimeUnit.MILLISECONDS);
        assertEquals(2, a.get());
        ts.advance(1500, TimeUnit.MILLISECONDS);
        assertEquals(3, a.get());

        // schedule with delay, disposed
        Disposable d = w.schedule(() -> a.set(4), 1, TimeUnit.SECONDS);
        d.dispose();
        assertEquals(3, a.get());
        ts.advance(1, TimeUnit.SECONDS);
        assertEquals(3, a.get());

        // schedule periodically
        d = w.schedulePeriodically(() -> a.incrementAndGet(), 1, 2, TimeUnit.SECONDS);
        assertEquals(3, a.get());
        ts.advance(1, TimeUnit.SECONDS);
        assertEquals(4, a.get());
        ts.advance(1, TimeUnit.SECONDS);
        assertEquals(4, a.get());
        ts.advance(1, TimeUnit.SECONDS);
        assertEquals(5, a.get());
        d.dispose();
        ts.advance(10, TimeUnit.SECONDS);
        assertEquals(5, a.get());

        // schedule periodically
        d = w.schedulePeriodically(() -> a.incrementAndGet(), 1, 2, TimeUnit.SECONDS);
        assertEquals(5, a.get());
        ts.advance(10, TimeUnit.SECONDS);
        assertEquals(10, a.get());
        
        //disposed worker does not run
        assertFalse(w.isDisposed());
        w.dispose();
        assertTrue(w.isDisposed());
        w.schedule(()-> a.set(0));
        assertNotEquals(0, a.get());
    }

}
