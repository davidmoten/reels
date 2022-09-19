package com.github.davidmoten.reels.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.davidmoten.reels.DisposedException;

public final class DisposedFuture<T> implements Future<T> {

    private static final DisposedFuture<Object> INSTANCE = new DisposedFuture<Object>();

    private DisposedFuture() {
        // prevent instantiation
    }

    @SuppressWarnings("unchecked")
    public static <T> DisposedFuture<T> instance() {
        return (DisposedFuture<T>) INSTANCE;
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
        return false;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        throw new DisposedException("already disposed, cannot create Future");
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new DisposedException("already disposed, cannot create Future");
    }

}
