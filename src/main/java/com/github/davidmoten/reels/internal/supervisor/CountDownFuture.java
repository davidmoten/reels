package com.github.davidmoten.reels.internal.supervisor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class CountDownFuture implements Future<Void> {

    private final CountDownLatch latch;

    public CountDownFuture(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return latch.getCount() > 0;
    }

    @Override
    public Void get() throws InterruptedException, ExecutionException {
        latch.await();
        return null;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (latch.await(timeout, unit)) {
            return null;
        } else {
            throw new TimeoutException("timed out");
        }
    }

}
