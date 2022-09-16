package com.github.davidmoten.reels.internal;

import java.util.concurrent.atomic.AtomicReference;

import com.github.davidmoten.reels.Disposable;

public final class OnDispose extends AtomicReference<Runnable> implements Disposable {

    private static final long serialVersionUID = 4825348210782788817L;

    public OnDispose(Runnable run) {
        super(run);
    }

    @Override
    public void dispose() {
        Runnable run = get();
        if (run != null) {
            if (compareAndSet(run, null)) {
                run.run();
            }
        }
    }

    @Override
    public boolean isDisposed() {
        return get() != null;
    }

}