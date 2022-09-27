package com.github.davidmoten.reels;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.davidmoten.reels.internal.ActorRefDisposed;
import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.Constants;
import com.github.davidmoten.reels.internal.DeadLetterActor;
import com.github.davidmoten.reels.internal.Heirarchy;
import com.github.davidmoten.reels.internal.Preconditions;
import com.github.davidmoten.reels.internal.RootActor;

/**
 * Creates actors, disposes actors and looks actors up by name.
 */
public final class Context implements Disposable {

//    private static final Logger log = LoggerFactory.getLogger(Context.class);

    public static final Context DEFAULT = new Context();

    private final Supervisor supervisor;

    private final AtomicLong counter = new AtomicLong();

    private final Object lock = new Object();

    private final AtomicInteger state = new AtomicInteger();

    private final Heirarchy actors;

    // actors active, can create
    private static final int STATE_ACTIVE = 0;

    // graceful shutdown via a poison pill to all actors, no more actor creation
    private static final int STATE_STOPPING = 1;

    // stop further activity, actors may be continuing with their current task
    private static final int STATE_DISPOSED = 2;

    // final state is TERMINATED indicated by latch having counted down to 0

    private final CompletableFuture<Void> terminated = new CompletableFuture<>();

    private final ActorRef<Object> deadLetterActor;

    final ActorRef<?> root;

    public Context() {
        this(Supervisor.defaultSupervisor());
    }

    public Context(Supervisor supervisor) {
        this(supervisor, () -> createActorObject(DeadLetterActor.class));
    }

    public Context(Supervisor supervisor, Supplier<? extends Actor<Object>> deadLetterActorFactory) {
        this.supervisor = supervisor;
        this.actors = new Heirarchy();
        this.root = createActor(RootActor.class);
        actors.setRoot(root);
        // must have set root before calling createActor
        this.deadLetterActor = createActor(deadLetterActorFactory, Constants.DEAD_LETTER_ACTOR_NAME);
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

    public ActorRef<Object> createActor(Supplier<? extends Actor<Object>> actorFactory, String name) {
        return createActor(actorFactory, name, Scheduler.defaultScheduler(), supervisor, Optional.ofNullable(root));
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn) {
        return createActor(actorClass, name, processMessagesOn, supervisor, Optional.ofNullable(root));
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn,
            Supervisor supervisor, Optional<ActorRef<?>> parent) {
        Preconditions.checkParameterNotNull(actorClass, "actorFactory");
        return createActor(() -> createActorObject(actorClass), name, processMessagesOn, supervisor, parent);
    }

    public <T> ActorRef<T> createActor(Supplier<? extends Actor<T>> actorFactory, String name,
            Scheduler processMessagesOn, Supervisor supervisor, Optional<ActorRef<?>> parent) {
        Preconditions.checkParameterNotNull(parent, "parent");
        return createActor(actorFactory, name, processMessagesOn, supervisor, parent.orElse(null));
    }

    <T> ActorRef<T> createActor(Supplier<? extends Actor<T>> actorFactory, String name, Scheduler processMessagesOn,
            Supervisor supervisor, ActorRef<?> parent) {
        Preconditions.checkParameterNotNull(actorFactory, "actorFactory");
        Preconditions.checkParameterNotNull(name, "name");
        Preconditions.checkParameterNotNull(processMessagesOn, "processMessagesOn");
        Preconditions.checkParameterNotNull(supervisor, "supervisor");
        if (state.get() != STATE_ACTIVE) {
            throw new CreateException("cannot create actor because Context shutdown has been called");
        }
        return insert(name, ActorRefImpl.create(name, actorFactory, processMessagesOn, this, supervisor, parent));
    }

    public <T> Optional<ActorRef<T>> lookupActor(String name) {
        return actors.get(name);
    }

    /**
     * Remove actor from context (so that when context is disposed actor.dispose()
     * is not called). Note that this method does not dispose the removed actor.
     * 
     * @param actorRefImpl name of the actor
     * @return the removed ActorRef
     */
    // TODO make internal method (called from ActorRefImpl)
    public boolean disposed(ActorRef<?> actor) {
        if (actors.remove(actor)) {
            if (state.get() != STATE_ACTIVE && actors.isEmpty()) {
                terminated.complete(null);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void dispose() {
        if (state.compareAndSet(STATE_ACTIVE, STATE_DISPOSED) || state.compareAndSet(STATE_STOPPING, STATE_DISPOSED)) {
            synchronized (lock) {
                actors.dispose();
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

    public CompletableFuture<Void> shutdownNow() throws InterruptedException, TimeoutException {
        dispose();
        return terminated;
    }

    public CompletableFuture<Void> shutdownGracefully() {
        if (state.compareAndSet(STATE_ACTIVE, STATE_STOPPING)) {
            synchronized (lock) {
                if (actors.allTerminated()) {
                    return terminated;
                } else {
                    actors.stop();
                }
            }
        }
        return terminated;
    }

    public ActorRef<Object> deadLetterActor() {
        return deadLetterActor;
    }

    // TODO internal
    public void actorStopped(ActorRefImpl<?> actor) {
        actors.actorStopped(actor);
        if (actors.allTerminated()) {
            terminated.complete(null);
        }
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
    public <T, S extends T> ActorBuilder<T> match(Class<S> matchClass, Consumer<? super Message<T>> consumer) {
        return this.<T>builder().match(matchClass, consumer);
    }

    public <T> ActorBuilder<T> matchAll(Consumer<? super Message<T>> consumer) {
        return this.<T>builder().matchAll(consumer);
    }
    
    public <T> ActorBuilder<T> matchEquals(T value, Consumer<? super Message<T>> consumer) {
        return this.<T>builder().matchEquals(value, consumer);
    }

    public <T> ActorBuilder<T> factory(Supplier<? extends Actor<T>> factory) {
        return this.<T>builder().factory(factory);
    }

    /////////////////////////////
    // private methods
    ////////////////////////////

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

    private <T> ActorRef<T> insert(String name, ActorRef<T> actor) {
        synchronized (lock) {
            if (state.get() != STATE_ACTIVE) {
                return new ActorRefDisposed<T>(this, name);
            } else {
                actors.add(actor);
                return actor;
            }
        }
    }

}
