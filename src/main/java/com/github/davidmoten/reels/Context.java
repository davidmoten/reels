package com.github.davidmoten.reels;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
public final class Context {

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

    private final ActorRefImpl<DeadLetter> deadLetterActor;

    final RootActorRefImpl root;

    private final Scheduler scheduler;

    private final MailboxFactory mailboxFactory;

    Context() {
        this(Supervisor.defaultSupervisor(), () -> createActorObject(DeadLetterActor.class),
                Scheduler.defaultScheduler(), MailboxFactory.unbounded());
    }

    Context(Supervisor supervisor, Supplier<? extends Actor<DeadLetter>> deadLetterActorFactory, Scheduler scheduler,
            MailboxFactory mailboxFactory) {
        this.supervisor = supervisor;
        // TODO this escaping the constructor
        this.root = new RootActorRefImpl(Constants.ROOT_ACTOR_NAME, ActorDoNothing::create, scheduler, this,
                supervisor);

        // set this before calling createActor
        this.mailboxFactory = mailboxFactory;
        this.scheduler = scheduler;

        this.deadLetterActor = (ActorRefImpl<DeadLetter>) createActor( //
                deadLetterActorFactory, //
                Constants.DEAD_LETTER_ACTOR_NAME, //
                Scheduler.defaultScheduler(), //
                supervisor, //
                Optional.ofNullable(root));
    }

    public static Context create() {
        return new Context();
    }

    public static Context create(Scheduler scheduler) {
        return builder().scheduler(scheduler).build();
    }

    public static ContextBuilder builder() {
        return new ContextBuilder();
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
        return createActor(actorFactory, name, processMessagesOn, supervisor, parent.orElse(null), mailboxFactory);
    }

    <T> ActorRef<T> createActor(Supplier<? extends Actor<T>> actorFactory, String name, Scheduler processMessagesOn,
            Supervisor supervisor, ActorRef<?> parent, MailboxFactory mailboxFactory) {
        Preconditions.checkArgumentNonNull(actorFactory, "actorFactory");
        Preconditions.checkArgumentNonNull(name, "name");
        Preconditions.checkArgumentNonNull(processMessagesOn, "processMessagesOn");
        Preconditions.checkArgumentNonNull(supervisor, "supervisor");
        if (state.get() != STATE_ACTIVE) {
            throw new CreateException("cannot create actor because Context shutdown/dispose has been called ");
        }
        return ActorRefImpl.create(name, actorFactory, processMessagesOn, this, supervisor, parent, mailboxFactory);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<ActorRef<T>> lookupActor(String name) {
        return Optional.ofNullable((ActorRef<T>) root.child(name));
    }

    public CompletableFuture<Void> shutdownGracefully() {
        state.compareAndSet(STATE_ACTIVE, STATE_STOPPING);
        root.stop();
        return root.stopFuture();
    }

    public CompletableFuture<Void> shutdownNow() {
        // cas loop to ensure dispose only gets called once
        while (true) {
            int s = state.get();
            if (s == STATE_DISPOSED) {
                break;
            } else if (state.compareAndSet(s, STATE_DISPOSED)) {
                root.stopNow();
                break;
            }
        }
        return root.stopFuture();
    }

    public Supervisor supervisor() {
        return supervisor;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public ActorRef<DeadLetter> deadLetterActor() {
        return deadLetterActor;
    }

    /////////////////////////////
    // builder entry methods
    ////////////////////////////

    public <T> ActorBuilder<T> actorBuilder() {
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
        return this.<T>actorBuilder().match(matchClass, consumer);
    }

    public <T> ActorBuilder<T> matchAny(Consumer<? super Message<T>> consumer) {
        return this.<T>actorBuilder().matchAny(consumer);
    }

    public <T> ActorBuilder<T> matchEquals(T value, Consumer<? super Message<T>> consumer) {
        return this.<T>actorBuilder().matchEquals(value, consumer);
    }

    public <T> ActorBuilder<T> actorFactory(Supplier<? extends Actor<T>> factory) {
        return this.<T>actorBuilder().actorFactory(factory);
    }

    public <T> ActorBuilder<T> actorClass(Class<? extends Actor<T>> actorClass, Object... args) {
        return this.<T>actorBuilder().actorClass(actorClass, args);
    }

    /////////////////////////////
    // private methods
    ////////////////////////////

    @SuppressWarnings("unchecked")
    static <T> Actor<T> createActorObject(Class<? extends Actor<T>> actorClass, Object... args) {
        Constructor<? extends Actor<T>> c = getMatchingConstructor(actorClass, args);
        return (Actor<T>) construct(c, args);
    }

    @SuppressWarnings("unchecked")
    static <C> Constructor<C> getMatchingConstructor(Class<C> c, Object[] args) {
        for (Constructor<?> con : c.getDeclaredConstructors()) {
            Class<?>[] types = con.getParameterTypes();
            if (types.length != args.length)
                continue;
            boolean match = true;
            for (int i = 0; i < types.length; i++) {
                Class<?> need = types[i], got = args[i].getClass();
                if (!need.isAssignableFrom(got)) {
                    if (need.isPrimitive()) {
                        match = int.class.equals(need) && Integer.class.equals(got) //
                                || long.class.equals(need) && Long.class.equals(got) //
                                || char.class.equals(need) && Character.class.equals(got) //
                                || short.class.equals(need) && Short.class.equals(got) //
                                || boolean.class.equals(need) && Boolean.class.equals(got) //
                                || byte.class.equals(need) && Byte.class.equals(got);
                    } else {
                        match = false;
                    }
                }
                if (!match)
                    break;
            }
            if (match)
                return (Constructor<C>) con;
        }
        throw new CreateException(
                "Cannot find an appropriate constructor for class " + c + " and arguments " + Arrays.toString(args));
    }

    // VisibleForTesting
    static Object construct(Constructor<?> c, Object... args) {
        try {
            return c.newInstance(args);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            throw new CreateException(e);
        }
    }

    public MailboxFactory mailboxFactory() {
        return mailboxFactory;
    }

}
