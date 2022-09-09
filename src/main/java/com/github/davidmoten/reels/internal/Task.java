package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.Disposable;

public final class Task implements Disposable, Runnable {

    private final Runnable run;
    private volatile boolean disposed;

    public Task(Runnable run) {
        this.run = run;
    }

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public void run() {
        if (!disposed) {
            run.run();
        }
    }
}
