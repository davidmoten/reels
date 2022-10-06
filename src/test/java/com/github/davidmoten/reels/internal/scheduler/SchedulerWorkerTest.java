package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Worker;

public class SchedulerWorkerTest {

    @Test
    public void test() {
        Worker w = Mockito.mock(Worker.class);
        SchedulerWorker s = new SchedulerWorker(w);
        assertFalse(s.isDisposed());
        s.dispose();
        assertTrue(s.isDisposed());
        // is disposed so will not schedule
        assertTrue(Disposable.disposed() == s.schedule(() -> {
        }));
        assertTrue(Disposable.disposed() == s.schedule(() -> {
        }, 10, TimeUnit.MILLISECONDS));
        assertTrue(Disposable.disposed() == s.schedulePeriodically(() -> {
        }, 1, 10, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSchedule() {
        Worker w = Mockito.mock(Worker.class);
        Runnable r = () -> {
        };
        Mockito.when(w.schedule(r)).thenReturn(Disposable.disposed());
        SchedulerWorker s = new SchedulerWorker(w);
        s.schedule(r);
        Mockito.verify(w, Mockito.times(1)).schedule(r);
        Mockito.verifyNoMoreInteractions(w);
    }

    @Test
    public void testScheduleNegativeDelay() {
        Worker w = Mockito.mock(Worker.class);
        Runnable r = () -> {
        };
        Mockito.when(w.schedule(r)).thenReturn(Disposable.disposed());
        SchedulerWorker s = new SchedulerWorker(w);
        s.schedule(r, -1, TimeUnit.SECONDS);
        Mockito.verify(w, Mockito.times(1)).schedule(r);
        Mockito.verifyNoMoreInteractions(w);
    }

    @Test
    public void testScheduleWithDelay() {
        Worker w = Mockito.mock(Worker.class);
        Runnable r = () -> {
        };
        Mockito.when(w.schedule(r, 1, TimeUnit.SECONDS)).thenReturn(Disposable.disposed());
        SchedulerWorker s = new SchedulerWorker(w);
        s.schedule(r, 1, TimeUnit.SECONDS);
        Mockito.verify(w, Mockito.times(1)).schedule(r, 1, TimeUnit.SECONDS);
        Mockito.verifyNoMoreInteractions(w);
    }

    @Test
    public void testSchedulePeriodically() {
        Worker w = Mockito.mock(Worker.class);
        Runnable r = () -> {
        };
        Mockito.when(w.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS)).thenReturn(Disposable.disposed());
        SchedulerWorker s = new SchedulerWorker(w);
        s.schedulePeriodically(r, 1, 1, TimeUnit.SECONDS);
        Mockito.verify(w, Mockito.times(1)).schedulePeriodically(r, 1, 1, TimeUnit.SECONDS);
        Mockito.verifyNoMoreInteractions(w);
    }

}
