package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.Disposed;

public interface Disposable {

    void dispose();
    
    boolean isDisposed();
    
    static Disposable disposed() {
        return Disposed.DISPOSED;
    }

}
