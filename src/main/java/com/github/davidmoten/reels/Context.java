package com.github.davidmoten.reels;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
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
        return createActor(actorClass, name, processMessagesOn, supervisor, Optional.empty());
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn,
            Supervisor supervisor, Optional<ActorRef<?>> parent) {
        Preconditions.checkArgument(name != null, "name cannot be null");
        try {
            Optional<Constructor<?>> c = Arrays.stream(actorClass.getConstructors())
                    .filter(x -> x.getParameterCount() == 0).findFirst();
            if (!c.isPresent()) {
                throw new CreateException(
                        "Actor class must have a public no-arg constructor to be created with this method."
                                + " Another method is available to create ActorRef for an Actor instance that you provide.");
            }
            @SuppressWarnings("unchecked")
            Actor<T> actor = (Actor<T>) c.get().newInstance();
            return createActor(actor, name, processMessagesOn, supervisor, parent);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new CreateException(e);
        }
    }

    public <T> ActorRef<T> createActor(Actor<T> actor, String name, Scheduler processMessagesOn,
            Supervisor supervisor, Optional<ActorRef<?>> parent) {
        Preconditions.checkArgument(name != null, "name cannot be null");
        if (disposed) {
            throw new CreateException("shutdown");
        }
        return insert(name, ActorRefImpl.create(name, actor, processMessagesOn, this, supervisor, parent));
    }

    private <T> ActorRef<T> insert(String name, ActorRef<T> actorRef) {
        Preconditions.checkArgument(name != null, "name cannot be null");
        actors.put(name, actorRef);
        return actorRef;
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass) {
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

    @Override
    public boolean isDisposed() {
        return disposed;
    }

}
