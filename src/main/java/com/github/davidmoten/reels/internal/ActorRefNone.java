package com.github.davidmoten.reels.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.DisposedException;
import com.github.davidmoten.reels.Scheduler;

public final class ActorRefNone implements ActorRef<Object> {
    
    public static final ActorRefNone NONE = new ActorRefNone() ;
    
    private ActorRefNone() {
        // prevent instantiation
    }

    @Override
    public void dispose() {
        // do nothing
    }

    @Override
    public boolean isDisposed() {
        return true;
    }

    @Override
    public void tell(Object message) {
        // do nothing        
    }

    @Override
    public void tell(Object message, ActorRef<?> sender) {
        // do nothing        
    }

    @Override
    public <S> CompletableFuture<S> ask(Object message) {
        CompletableFuture<S> f = new CompletableFuture<S>() {};
        f.completeExceptionally(new DisposedException("recipient actor disposed"));
        return f;
    }

    @Override
    public void stop() {
        // do nothing
    }

    @Override
    public Context context() {
        return Context.DEFAULT;
    }

    @Override
    public String name() {
        return "reels-none";
    }

    @Override
    public Scheduler scheduler() {
        return context().scheduler();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ActorRef<S> parent() {
        return (ActorRef<S>) this;
    }

    @Override
    public <S> ActorRef<S> child(String name) {
        return null;
    }

    @Override
    public <S> Collection<ActorRef<S>> children() {
        return Collections.emptyList();
    }

}
