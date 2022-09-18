package com.github.davidmoten.reels.internal;

import java.util.concurrent.atomic.AtomicBoolean;

import com.github.davidmoten.reels.Disposable;

public final class OnDispose extends AtomicBoolean implements Disposable {

    private static final long serialVersionUID = 7568424680173588693L;
    
    private final Runnable run;

    public OnDispose(Runnable run) {
        this.run = run;
    }
    
    @Override
    public void dispose() {
        if (compareAndSet(false, true)) {
            run.run();
        }
    }

    @Override
    public boolean isDisposed() {
        return get();
    }

}
