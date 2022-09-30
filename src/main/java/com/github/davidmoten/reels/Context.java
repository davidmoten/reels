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

import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.Constants;
import com.github.davidmoten.reels.internal.DeadLetterActor;
import com.github.davidmoten.reels.internal.Preconditions;
import com.github.davidmoten.reels.internal.RootActorRefImpl;

/**
 * Creates actors, disposes actors and looks actors up by name.
 */
public final class Context implements Disposable {

//    private static final Logger log = LoggerFactory.getLogger(Context.class);

    public static final Context DEFAULT = new Context();

    private final Supervisor supervisor;

    private final AtomicLong counter = new AtomicLong();

    private final AtomicInteger state = new AtomicInteger();

    // actors active, can create
    private static final int STATE_ACTIVE = 0;

    // graceful shutdown via a poison pill to all actors, no more actor creation
    private static final int STATE_STOPPING = 1;

    // stop further activity, actors may be continuing with their current task
    private static final int STATE_DISPOSED = 2;

    // final state is TERMINATED indicated by latch having counted down to 0

    private final ActorRefImpl<Object> deadLetterActor;

    final RootActorRefImpl root;

    public Context() {
        this(Supervisor.defaultSupervisor());
    }

    public Context(Supervisor supervisor) {
        this(supervisor, () -> createActorObject(DeadLetterActor.class));
    }

    public Context(Supervisor supervisor, Supplier<? extends Actor<Object>> deadLetterActorFactory) {
        this.supervisor = supervisor;
        // TODO this escaping the constructor
        this.root = new RootActorRefImpl(Constants.ROOT_ACTOR_NAME, deadLetterActorFactory,
                Scheduler.defaultScheduler(), this, supervisor);
        this.deadLetterActor = (ActorRefImpl<Object>) createActor(deadLetterActorFactory,
                Constants.DEAD_LETTER_ACTOR_NAME);
    }

    public static Context create() {
        return new Context();
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass) {
        return createActor(actorClass, actorClass.getName() + "-" + Long.toString(counter.incrementAndGet()));
    }

    public <T> ActorRef<T> createActor(Supplier<? extends Actor<T>> factory) {
        return createActor(factory, "Anonymous-" + Long.toString(counter.incrementAndGet()));
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name) {
        return createActor(actorClass, name, Scheduler.defaultScheduler());
    }

    public <T> ActorRef<T> createActor(Supplier<? extends Actor<T>> actorFactory, String name) {
        return createActor(actorFactory, name, Scheduler.defaultScheduler(), supervisor, Optional.ofNullable(root));
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn) {
        return createActor(actorClass, name, processMessagesOn, supervisor, Optional.ofNullable(root));
    }

    public <T> ActorRef<T> createActor(Class<? extends Actor<T>> actorClass, String name, Scheduler processMessagesOn,
            Supervisor supervisor, Optional<ActorRef<?>> parent) {
        Preconditions.checkArgumentNonNull(actorClass, "actorClass");
        return createActor(() -> createActorObject(actorClass), name, processMessagesOn, supervisor, parent);
    }

    public <T> ActorRef<T> createActor(Supplier<? extends Actor<T>> actorFactory, String name,
            Scheduler processMessagesOn, Supervisor supervisor, Optional<ActorRef<?>> parent) {
        Preconditions.checkArgumentNonNull(parent, "parent");
        return createActor(actorFactory, name, processMessagesOn, supervisor, parent.orElse(null));
    }

    <T> ActorRef<T> createActor(Supplier<? extends Actor<T>> actorFactory, String name, Scheduler processMessagesOn,
            Supervisor supervisor, ActorRef<?> parent) {
        Preconditions.checkArgumentNonNull(actorFactory, "actorFactory");
        Preconditions.checkArgumentNonNull(name, "name");
        Preconditions.checkArgumentNonNull(processMessagesOn, "processMessagesOn");
        Preconditions.checkArgumentNonNull(supervisor, "supervisor");
        if (state.get() != STATE_ACTIVE) {
            throw new CreateException("cannot create actor because Context shutdown/dispose has been called ");
        }
        return ActorRefImpl.create(name, actorFactory, processMessagesOn, this, supervisor, parent);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ActorRef<T>> lookupActor(String name) {
        return Optional.ofNullable((ActorRef<T>) root.child(name));
    }

    @Override
    public void dispose() {
        // cas loop to ensure dispose only gets called once
        while (true) {
            int s = state.get();
            if (s == STATE_DISPOSED) {
                return;
            } else if (state.compareAndSet(s, STATE_DISPOSED)) {
                break;
            }
        }
        root.dispose();
        root.stopFuture().completeExceptionally(new DisposedException("dispose has been called"));
    }

    @Override
    public boolean isDisposed() {
        return state.get() == STATE_DISPOSED;
    }

    public Supervisor supervisor() {
        return supervisor;
    }

    public void shutdownNow() throws InterruptedException, TimeoutException {
        dispose();
    }

    public CompletableFuture<Void> shutdownGracefully() {
        state.compareAndSet(STATE_ACTIVE, STATE_STOPPING);
        return root.stopFuture();
    }

    public ActorRef<Object> deadLetterActor() {
        return deadLetterActor;
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

    public <T> ActorBuilder<T> matchAny(Consumer<? super Message<T>> consumer) {
        return this.<T>builder().matchAny(consumer);
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
        Optional<Constructor<?>> c = Arrays.stream(actorClass.getConstructors()).filter(x -> x.getParameterCount() == 0)
                .findFirst();
        if (!c.isPresent()) {
            throw new CreateException(
                    "Actor class must have a public no-arg constructor to be created with this method."
                            + " Another method is available to create ActorRef for an Actor instance that you provide.");
        }
        return (Actor<T>) construct(c.get());
    }

    // VisibleForTesting
    static Object construct(Constructor<?> c) {
        try {
            return c.newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new CreateException(e);
        }
    }
}
