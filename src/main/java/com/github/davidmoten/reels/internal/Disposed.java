package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.Disposable;

public final class Disposed implements Disposable {

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public boolean isDisposed() {
        return true;
    }
}