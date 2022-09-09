package com.github.davidmoten.reels;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.SupervisorDefault;

/**
 * Creates actors, disposes actors and looks actors up by name.
 */
public final class Context {

    private final Supervisor supervisor;

    private final AtomicLong counter = new AtomicLong();
    private final Map<String, ActorRef<?>> actors = new ConcurrentHashMap<>();

    public Context(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public Context() {
        this(SupervisorDefault.INSTANCE);
    }

    public <T> ActorRef<T> create(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn) {
        return create(actorClass, name, processMessagesOn, supervisor);
    }

    public <T> ActorRef<T> create(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn,
            Supervisor supervisor) {
        Preconditions.checkArgument(name != null, "name cannot be null");
        try {
            return create((Actor<T>) actorClass.newInstance(), name, processMessagesOn, supervisor);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new CreateException(e);
        }
    }

    public <T> ActorRef<T> create(Actor<T> actor, String name, Scheduler processMessagesOn, Supervisor supervisor) {
        Preconditions.checkArgument(name != null, "name cannot be null");
        return insert(name, new ActorRefImpl<T>(name, actor, processMessagesOn, this, supervisor));
    }

    private <T> ActorRef<T> insert(String name, ActorRef<T> actorRef) {
        Preconditions.checkArgument(name != null, "name cannot be null");
        actors.put(name, actorRef);
        return actorRef;
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

    public void disposeActor(String name) {
        actors.remove(name);
    }

}
