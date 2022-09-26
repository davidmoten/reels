package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Test;

public class CancelledCompletableFutureTest {
    
    @Test
    public void test() throws InterruptedException, ExecutionException, TimeoutException {
        CancelledCompletableFuture<Object> d = CancelledCompletableFuture.instance();
        assertTrue(d.isCancelled());
        assertTrue(d.cancel(true));
        assertTrue(d.isCancelled());
        assertTrue(d.isDone());
        try {
            d.get();
            Assert.fail();
        } catch (CancellationException e) {
            // all good
        }
        try {
            d.get(100, TimeUnit.MILLISECONDS);
            Assert.fail();
        } catch (CancellationException e) {
            // all good
        }
    }

}
