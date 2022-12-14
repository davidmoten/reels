package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.Future;

import com.github.davidmoten.reels.Disposable;

public final class FutureTask implements Disposable {

    private final Future<?> future;

    public FutureTask(Future<?> future) {
        this.future = future;
    }
    
    @Override
    public void dispose() {
        future.cancel(false);
    }
    
    @Override
    public boolean isDisposed() {
        return future.isCancelled();
    }
}
