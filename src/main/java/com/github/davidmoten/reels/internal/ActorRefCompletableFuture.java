package com.github.davidmoten.reels.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Scheduler;

public class ActorRefCompletableFuture<T> extends CompletableFuture<T> implements ActorRef<T> {
    
    @Override
    public void dispose() {
        
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void tell(T message) {
        tell(message, ActorRef.none());
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        this.complete(message);
    }

    @Override
    public <S> CompletableFuture<S> ask(T message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void stop() {
        
    }

    @Override
    public Context context() {
        return Context.DEFAULT;
    }

    @Override
    public String name() {
        return "reels-actor-ref-completable-future";    
    }

    @Override
    public Scheduler scheduler() {
        return context().scheduler();
    }

    @Override
    public <S> ActorRef<S> parent() {
        return ActorRef.none();
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
