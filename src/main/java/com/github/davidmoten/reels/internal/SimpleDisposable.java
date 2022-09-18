package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.Disposable;

public final class SimpleDisposable implements Disposable {

    private volatile boolean disposed;

    @Override
    public void dispose() {
        disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

}
