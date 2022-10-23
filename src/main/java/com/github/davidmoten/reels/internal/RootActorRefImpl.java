package com.github.davidmoten.reels.internal;


import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.MailboxFactory;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Supervisor;

public final class RootActorRefImpl extends ActorRefSynchronized<Object> {
    
    private final CompletableFuture<Void> stopFuture = new CompletableFuture<>();

    public RootActorRefImpl(String name, Supplier<? extends Actor<Object>> factory, Scheduler scheduler,
            Context context, Supervisor supervisor) {
        super(name, factory, scheduler, context, supervisor, null, MailboxFactory.unbounded());
    }

    public CompletableFuture<Void> stopFuture() {
        stop();
        return stopFuture;
    }

    @Override
    protected void complete() {
        stopFuture.complete(null);
    }
}
