package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.Disposed;
import com.github.davidmoten.reels.internal.OnDispose;

public interface Disposable {

    void dispose();
    
    boolean isDisposed();
    
    static Disposable disposed() {
        return Disposed.DISPOSED;
    }

    static Disposable onDispose(Runnable run) {
        return new OnDispose(run);
    }

}
