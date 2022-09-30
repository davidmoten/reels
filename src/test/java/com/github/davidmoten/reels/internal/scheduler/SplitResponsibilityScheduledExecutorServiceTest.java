package com.github.davidmoten.reels.internal.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.Mockito;

public class SplitResponsibilityScheduledExecutorServiceTest {

    @Test
    public void testExecute() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        Runnable r = mock(Runnable.class);
        s.execute(r);
        verify(a, times(1)).execute(r);
        verifyNoMoreInteractions(a);
        verifyNoInteractions(b);
    }

    @Test
    public void testSubmit() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        Runnable r = mock(Runnable.class);
        s.submit(r);
        verify(a, times(1)).submit(r);
        verifyNoMoreInteractions(a);
        verifyNoInteractions(b);
    }

    @Test
    public void testSubmitCallable() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        @SuppressWarnings("unchecked")
        Callable<Object> r = mock(Callable.class);
        s.submit(r);
        verify(a, times(1)).submit(r);
        verifyNoMoreInteractions(a);
        verifyNoInteractions(b);
    }

    @Test
    public void testShutdown() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        s.shutdown();
        verify(a, times(1)).shutdown();
        verify(b, times(1)).shutdown();
        verifyNoMoreInteractions(a);
        verifyNoMoreInteractions(b);
    }

    @Test
    public void testSchedule() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        Runnable r = mock(Runnable.class);
        s.schedule(r, 1L, TimeUnit.SECONDS);
        verify(b, times(1)).schedule(Mockito.<Runnable>any(), eq(1L), eq(TimeUnit.SECONDS));
        verifyNoMoreInteractions(b);
        verifyNoInteractions(a);
    }

    @Test
    public void testIsShutdown() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);

        when(a.isShutdown()).thenReturn(true);
        when(b.isShutdown()).thenReturn(true);
        assertTrue(s.isShutdown());

        reset(a);
        reset(b);
        when(a.isShutdown()).thenReturn(true);
        when(b.isShutdown()).thenReturn(false);
        assertFalse(s.isShutdown());

        reset(a);
        reset(b);
        when(a.isShutdown()).thenReturn(false);
        when(b.isShutdown()).thenReturn(true);
        assertFalse(s.isShutdown());

        reset(a);
        reset(b);
        when(a.isShutdown()).thenReturn(false);
        when(b.isShutdown()).thenReturn(false);
        assertFalse(s.isShutdown());
    }

    @Test
    public void testIsTerminated() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);

        when(a.isTerminated()).thenReturn(true);
        when(b.isTerminated()).thenReturn(true);
        assertTrue(s.isTerminated());

        reset(a);
        reset(b);
        when(a.isTerminated()).thenReturn(true);
        when(b.isTerminated()).thenReturn(false);
        assertFalse(s.isTerminated());

        reset(a);
        reset(b);
        when(a.isTerminated()).thenReturn(false);
        when(b.isTerminated()).thenReturn(true);
        assertFalse(s.isTerminated());

        reset(a);
        reset(b);
        when(a.isTerminated()).thenReturn(false);
        when(b.isTerminated()).thenReturn(false);
        assertFalse(s.isTerminated());
    }
    
}
