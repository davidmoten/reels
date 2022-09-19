package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.Disposed;
import com.github.davidmoten.reels.internal.OnDispose;
import com.github.davidmoten.reels.internal.SimpleDisposable;

public interface Disposable {

    void dispose();
    
    boolean isDisposed();
    
    static Disposable disposed() {
        return Disposed.DISPOSED;
    }
    
    static Disposable simple() {
        return new SimpleDisposable();
    }
    
    static Disposable onDispose(Runnable run) {
        return new OnDispose(run);
    }

}
