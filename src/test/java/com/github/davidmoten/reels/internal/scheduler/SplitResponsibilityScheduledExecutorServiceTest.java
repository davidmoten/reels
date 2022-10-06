package com.github.davidmoten.reels.internal.scheduler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
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
    public void testSubmitWithValue() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        Runnable r = mock(Runnable.class);
        s.submit(r, 10);
        verify(a, times(1)).submit(r, 10);
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
    public void testScheduleCallable() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        @SuppressWarnings("unchecked")
        Callable<Object> r = mock(Callable.class);
        s.schedule(r, 1L, TimeUnit.SECONDS);
        verify(b, times(1)).schedule(Mockito.<Callable<Object>>any(), eq(1L), eq(TimeUnit.SECONDS));
        verifyNoMoreInteractions(b);
        verifyNoInteractions(a);
    }
    
    @Test
    public void testScheduleAtFixedRate() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        Runnable r = mock(Runnable.class);
        s.scheduleAtFixedRate(r, 1L, 1L,TimeUnit.SECONDS);
        verify(b, times(1)).scheduleAtFixedRate(Mockito.<Runnable>any(), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
        verifyNoMoreInteractions(b);
        verifyNoInteractions(a);
    }
    
    @Test
    public void testScheduleWithFixedDelay() {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        Runnable r = mock(Runnable.class);
        s.scheduleWithFixedDelay(r, 1L, 1L,TimeUnit.SECONDS);
        verify(b, times(1)).scheduleWithFixedDelay(Mockito.<Runnable>any(), eq(1L), eq(1L), eq(TimeUnit.SECONDS));
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
    @Ignore
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
    
    @Test
    public void testAwaitTermination() throws InterruptedException {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);

        when(a.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(true);
        when(b.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(true);
        assertTrue(s.awaitTermination(1, TimeUnit.SECONDS));

        reset(a);
        reset(b);
        when(a.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(true);
        when(b.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(false);
        assertFalse(s.awaitTermination(1, TimeUnit.SECONDS));

        reset(a);
        reset(b);
        when(a.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(false);
        when(b.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(true);
        assertFalse(s.awaitTermination(1, TimeUnit.SECONDS));

        reset(a);
        reset(b);
        when(a.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(false);
        when(b.awaitTermination(1, TimeUnit.SECONDS)).thenReturn(false);
        assertFalse(s.awaitTermination(1, TimeUnit.SECONDS));
    }
 
    @Test
    public void testInvokeAll() throws InterruptedException {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        List<Callable<Object>> list = Arrays.asList(() -> null);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        s.invokeAll(list);
        verify(a, times(1)).invokeAll(list);
        verifyNoMoreInteractions(a);
        verifyNoInteractions(b);
    }
    
    @Test
    public void testInvokeAllWithTimeout() throws InterruptedException {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        List<Callable<Object>> list = Arrays.asList(() -> null);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        s.invokeAll(list, 1, TimeUnit.SECONDS);
        verify(b, times(1)).invokeAll(eq(list), eq(1L), eq(TimeUnit.SECONDS));
        verifyNoInteractions(a);
        verifyNoMoreInteractions(b);
    }
    
    @Test
    public void testInvokeAny() throws InterruptedException, ExecutionException {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        List<Callable<Object>> list = Arrays.asList(() -> null);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        s.invokeAny(list);
        verify(a, times(1)).invokeAny(list);
        verifyNoMoreInteractions(a);
        verifyNoInteractions(b);
    }
    
    @Test
    public void testInvokeAnyWithTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService a = mock(ExecutorService.class);
        ScheduledExecutorService b = mock(ScheduledExecutorService.class);
        List<Callable<Object>> list = Arrays.asList(() -> null);
        SplitResponsibilityScheduledExecutorService s = new SplitResponsibilityScheduledExecutorService(a, b);
        s.invokeAny(list, 1, TimeUnit.SECONDS);
        verify(b, times(1)).invokeAny(eq(list), eq(1L), eq(TimeUnit.SECONDS));
        verifyNoInteractions(a);
        verifyNoMoreInteractions(b);
    }
 
}
