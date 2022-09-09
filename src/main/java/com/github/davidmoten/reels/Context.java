package com.github.davidmoten.reels;

import java.util.concurrent.atomic.AtomicLong;

public final class Context {

    private final Supervisor supervisor;

    private final AtomicLong counter = new AtomicLong();

    public Context(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public <T> ActorRef<T> create(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn) {
        return null;
    }

    public <T> ActorRef<T> create(Class<? extends Actor<T>> actorClass) {
        return create(actorClass, Long.toString(counter.incrementAndGet()), Scheduler.computation());
    }

    public <T> ActorRef<T> actorRef(String name, Class<T> messageType) {
        return null;
    }

}
