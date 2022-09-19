package com.github.davidmoten.reels;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.reels.internal.ActorRefDisposed;
import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.supervisor.CountDownFuture;
import com.github.davidmoten.reels.internal.supervisor.DoneFuture;

/**
 * Creates actors, disposes actors and looks actors up by name.
 */
public final class Context implements Disposable {

    public static final Context DEFAULT = new Context();

    private final Supervisor supervisor;

    private final AtomicLong counter = new AtomicLong();

    private final Object lock = new Object();

    private final Map<String, ActorRef<?>> actors = new ConcurrentHashMap<>();

    private final AtomicInteger state = new AtomicInteger();

    // actors active, can create
    private static final int STATE_ACTIVE = 0;

    // graceful shutdown via a poison pill to all actors, no more actor creation
    private static final int STATE_STOPPING = 1;

    // stop further activity, actors may be continuing with their current task
    private static final int STATE_DISPOSED = 2;

    // final state is TERMINATED indicated by latch having counted down to 0

    private final CountDownLatch latch = new CountDownLatch(1);

    public Context() {
        this(Supervisor.defaultSupervisor());
    }

    public Context(Supervisor supervisor) {
        this.supervisor = supervisor;
    }

    public static Context create() {
        return new Context();
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass) {
        return createActor(actorClass, actorClass.getName() + "-" + Long.toString(counter.incrementAndGet()));
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name) {
        return createActor(actorClass, name, Scheduler.defaultScheduler());
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn) {
        return createActor(actorClass, name, processMessagesOn, supervisor, Optional.empty());
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn,
            Supervisor supervisor, Optional<ActorRef<?>> parent) {
        Preconditions.checkArgument(actorClass != null, "actorFactory cannot be null");
        return createActor(() -> createActorObject(actorClass), name, processMessagesOn, supervisor, parent);
    }

    public <T> ActorRef<T> createActor(Supplier<? extends Actor<T>> actorFactory, String name,
            Scheduler processMessagesOn, Supervisor supervisor, Optional<ActorRef<?>> parent) {
        Preconditions.checkArgument(actorFactory != null, "actorFactory cannot be null");
        Preconditions.checkArgument(name != null, "name cannot be null");
        Preconditions.checkArgument(processMessagesOn != null, "processMessagesOn scheduler cannot be null");
        Preconditions.checkArgument(supervisor != null, "supervisor cannot be null");
        Preconditions.checkArgument(parent != null, "parent cannot be null");
        if (state.get() != STATE_ACTIVE) {
            throw new CreateException("cannot create actor because Context shutdown");
        }
        return insert(name, ActorRefImpl.create(name, actorFactory, processMessagesOn, this, supervisor, parent));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ActorRef<T>> lookupActor(String name) {
        return Optional.ofNullable((ActorRef<T>) actors.get(name));
    }

    /**
     * Remove actor from context (so that when context is disposed actor.dispose()
     * is not called). Note that this method does not dispose the removed actor.
     * 
     * @param name name of the actor
     * @return the removed ActorRef
     */
    // TODO make internal method (called from ActorRefImpl)
    public boolean removeActor(String name) {
        ActorRef<?> a = actors.remove(name);
        if (a == null) {
            return false;
        } else if (state.get() != STATE_ACTIVE && actors.isEmpty()) {
            latch.countDown();
        }
        return true;
    }

    @Override
    public void dispose() {
        if (state.compareAndSet(STATE_ACTIVE, STATE_DISPOSED) || state.compareAndSet(STATE_STOPPING, STATE_DISPOSED)) {
            synchronized (lock) {
                actors.forEach((name, actorRef) -> actorRef.dispose());
                actors.clear();
            }
        }
    }

    @Override
    public boolean isDisposed() {
        return state.get() == STATE_DISPOSED;
    }
    
    public Supervisor supervisor() {
        return supervisor;
    }
    
    public Future<Void> shutdownNow() throws InterruptedException, TimeoutException {
        dispose();
        return new CountDownFuture(latch);
    }

    public Future<Void> shutdownGracefully() {
        if (state.compareAndSet(STATE_ACTIVE, STATE_STOPPING)) {
            synchronized (lock) {
                if (actors.isEmpty()) {
                    return new DoneFuture();
                } else {
                    actors.values().forEach(actor -> actor.stop());
                }
            }
        }
        return new CountDownFuture(latch);
    }

    /////////////////////////////
    // builder entry methods
    ////////////////////////////

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
     * @return builder builder
     */
    public <T, S extends T> ActorBuilder<T> match(Class<S> matchClass, BiConsumer<MessageContext<T>, S> consumer) {
        return this.<T>builder().match(matchClass, consumer);
    }

    public <T> ActorBuilder<T> matchAll(BiConsumer<MessageContext<T>, ? super T> consumer) {
        return this.<T>builder().matchAll(consumer);
    }

    public <T> ActorBuilder<T> factory(Supplier<? extends Actor<T>> factory) {
        return this.<T>builder().factory(factory);
    }
    
    @SuppressWarnings("unchecked")
    private static <T> Actor<T> createActorObject(Class<? extends Actor<T>> actorClass) {
        Preconditions.checkArgument(actorClass != null, "actorFactory cannot be null");
        try {
            Optional<Constructor<?>> c = Arrays.stream(actorClass.getConstructors())
                    .filter(x -> x.getParameterCount() == 0).findFirst();
            if (!c.isPresent()) {
                throw new CreateException(
                        "Actor class must have a public no-arg constructor to be created with this method."
                                + " Another method is available to create ActorRef for an Actor instance that you provide.");
            }
            return (Actor<T>) c.get().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new CreateException(e);
        }
    }

    private <T> ActorRef<T> insert(String name, ActorRef<T> actorRef) {
        synchronized (lock) {
            if (state.get() != STATE_ACTIVE) {
                return new ActorRefDisposed<T>(this, name);
            } else {
                actors.put(name, actorRef);
                return actorRef;
            }
        }
    }
    
}
