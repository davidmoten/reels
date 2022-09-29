package com.github.davidmoten.reels.internal;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Supervisor;

public final class RootActorRefImpl extends ActorRefImpl<Object> {

    private static final long serialVersionUID = -7059590596057714328L;
    
    private final CompletableFuture<Void> stopFuture = new CompletableFuture<>();
    
    public RootActorRefImpl(String name, Supplier<? extends Actor<Object>> factory, Scheduler scheduler, Context context,
            Supervisor supervisor) {
        super(name, factory, scheduler, context, supervisor, null);
    }
    
    public CompletableFuture<Void> stopFuture() {
        if (state == ACTIVE) {
            stop();
        }
        return stopFuture;
    }

    @Override
    protected void complete() {
        stopFuture.complete(null);
    }
}
