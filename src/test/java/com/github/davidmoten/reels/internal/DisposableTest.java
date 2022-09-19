package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.davidmoten.reels.Disposable;

public class DisposableTest {
    
    @Test
    public void testDisposed() {
        Disposable d = Disposable.disposed();
        assertTrue(d.isDisposed());
        d.dispose();
        assertTrue(d.isDisposed());
    }

}
