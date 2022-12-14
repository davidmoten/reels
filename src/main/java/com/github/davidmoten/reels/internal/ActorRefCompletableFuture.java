package com.github.davidmoten.reels.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Scheduler;

public final class ActorRefCompletableFuture<T> extends CompletableFuture<T> implements ActorRef<T> {
    
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
        // do nothing
    }

    @Override
    public Context context() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String name() {
        return "reels-actor-ref-completable-future";    
    }

    @Override
    public Scheduler scheduler() {
        throw new UnsupportedOperationException();
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

    @Override
    public void stopNow() {
    }

    @Override
    public boolean isStopped() {
        return false;
    }

}
