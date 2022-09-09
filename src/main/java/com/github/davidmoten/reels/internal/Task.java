package com.github.davidmoten.reels.internal;

import java.util.concurrent.Future;

import com.github.davidmoten.reels.Disposable;

public final class Task implements Disposable {

    private final Future<?> future;

    public Task(Future<?> future) {
        this.future = future;
    }

    @Override
    public void dispose() {
        this.future.cancel(false);
    }
}
