package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.github.davidmoten.reels.Disposable;

public class OnDisposeTest {
    
    @Test
    public void test() {
        AtomicInteger count = new AtomicInteger();
        Disposable d = Disposable.onDispose(() -> count.incrementAndGet());
        assertFalse(d.isDisposed());
        d.dispose();
        assertTrue(d.isDisposed());
        d.dispose();
        assertTrue(d.isDisposed());
        assertEquals(1, count.get());
    }

}
