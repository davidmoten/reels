package com.github.davidmoten.reels.internal;

import java.util.concurrent.CompletableFuture;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Scheduler;

public final class ActorRefDisposed<T> implements ActorRef<T> {

    private final Context context;
    private final String name;

    public ActorRefDisposed(Context context, String name) {
        this.context = context;
        this.name = name;
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
    public void tell(T message) {
        // do nothing
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        // do nothing
    }

    @Override
    public <S> CompletableFuture<S> ask(T message) {
        return CancelledCompletableFuture.instance();
    }

    @Override
    public void stop() {
        // do nothing
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Scheduler scheduler() {
        return Scheduler.doNothing();
    }

    @Override
    public ActorRef<?> parent() {
        return null;
    }

}
