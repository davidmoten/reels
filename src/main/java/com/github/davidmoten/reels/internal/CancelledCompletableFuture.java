package com.github.davidmoten.reels.internal;

import java.util.concurrent.CompletableFuture;

public final class CancelledCompletableFuture<T> extends CompletableFuture<T> {

    private static final CancelledCompletableFuture<Object> INSTANCE = create();

    private CancelledCompletableFuture() {
        // prevent instantiation
        super();
    }
    
    private static CancelledCompletableFuture<Object> create() {
        CancelledCompletableFuture<Object> d = new CancelledCompletableFuture<>();
        d.cancel(false);
        return d;
    }

    @SuppressWarnings("unchecked")
    public static <T> CancelledCompletableFuture<T> instance() {
        return (CancelledCompletableFuture<T>) INSTANCE;
    }

}
