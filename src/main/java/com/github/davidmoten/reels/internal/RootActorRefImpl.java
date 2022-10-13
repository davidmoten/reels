package com.github.davidmoten.reels.internal;


import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Supervisor;

public final class RootActorRefImpl extends ActorRefSerialized<Object> {
    
    private static final Logger log = LoggerFactory.getLogger(RootActorRefImpl.class);

    private final CompletableFuture<Void> stopFuture = new CompletableFuture<>();

    public RootActorRefImpl(String name, Supplier<? extends Actor<Object>> factory, Scheduler scheduler,
            Context context, Supervisor supervisor) {
        super(name, factory, scheduler, context, supervisor, null);
    }

    public CompletableFuture<Void> stopFuture() {
        stop();
        return stopFuture;
    }

    @Override
    protected void complete() {
        if (ActorRefImpl.debug) {
            log.debug("completing root future");
        }
        stopFuture.complete(null);
    }
}
