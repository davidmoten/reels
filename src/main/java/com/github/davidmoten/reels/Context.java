package com.github.davidmoten.reels;

import java.util.concurrent.atomic.AtomicLong;

import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.SupervisorDefault;

public final class Context {

    private final Supervisor supervisor;

    private final AtomicLong counter = new AtomicLong();

    public Context(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public Context() {
        this(SupervisorDefault.INSTANCE);
    }

    public <T> ActorRef<T> create(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn) {
        try {
            return new ActorRefImpl<>(actorClass.newInstance(), processMessagesOn, this, supervisor);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CreateException(e);
        }
    }

    public <T> ActorRef<T> create(Class<? extends Actor<T>> actorClass) {
        return create(actorClass, Long.toString(counter.incrementAndGet()), Scheduler.computation());
    }
    
    public <T> ActorBuilder<T> messageClass(Class<T> messageClass) {
        return new ActorBuilder<T>(this);
    }

    public <T> ActorRef<T> lookupActor(String name, Class<T> messageType) {
        return null;
    }

}
