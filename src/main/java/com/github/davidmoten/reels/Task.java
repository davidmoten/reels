package com.github.davidmoten.reels;

import java.util.concurrent.Future;

final class Task implements Disposable {

    private final Future<?> future;

    Task(Future<?> future) {
        this.future = future;
    }

    @Override
    public void dispose() {
        this.future.cancel(false);
    }
}
