package com.github.davidmoten.reels;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.SupervisorDefault;

/**
 * Creates actors, disposes actors and looks actors up by name.
 */
public final class Context implements Disposable {

    public static final Context DEFAULT = new Context();

    private final Supervisor supervisor;

    private final AtomicLong counter = new AtomicLong();
    private final Map<String, ActorRef<?>> actors = new ConcurrentHashMap<>();

    private volatile boolean disposed;

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
        if (disposed) {
            throw new CreateException("shutdown");
        }
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

    public <T> ActorBuilder<T> builder() {
        return new ActorBuilder<T>(this);
    }

    /**
     * Returns an ActorBuilder using the given match.
     * 
     * @param <T>        actor message type
     * @param <S>        match class type
     * @param matchClass match class
     * @param consumer   consumes messages of type S
     * @return builder
     */
    public <T, S extends T> ActorBuilder<T> match(Class<S> matchClass, BiConsumer<MessageContext<T>, S> consumer) {
        return new ActorBuilder<T>(this).match(matchClass, consumer);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ActorRef<T>> lookupActor(String name) {
        return Optional.ofNullable((ActorRef<T>) actors.get(name));
    }

    public void disposeActor(String name) {
        ActorRef<?> a = actors.remove(name);
        if (a != null) {
            a.dispose();
        }
    }

    @Override
    public void dispose() {
        if (!disposed) {
            disposed = true;
            actors.forEach((name, actorRef) -> actorRef.dispose());
            actors.clear();
        }
    }

}
