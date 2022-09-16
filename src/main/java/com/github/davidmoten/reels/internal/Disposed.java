package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.Disposable;

public enum Disposed implements Disposable {

    DISPOSED;
    
    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public boolean isDisposed() {
        return true;
    }
}