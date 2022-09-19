package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.reels.DisposedException;

public class DisposedFutureTest {
    
    @Test
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        DisposedFuture<Object> d = DisposedFuture.instance();
        assertFalse(d.isCancelled());
        assertFalse(d.cancel(true));
        assertFalse(d.isCancelled());
        assertFalse(d.isDone());
        try {
            d.get();
            Assert.fail();
        } catch (DisposedException e) {
            // all good
        }
        try {
            d.get(100, TimeUnit.MILLISECONDS);
            Assert.fail();
        } catch (DisposedException e) {
            // all good
        }
    }

}
