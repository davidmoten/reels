package com.github.davidmoten.reels;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.Preconditions;
import com.github.davidmoten.reels.internal.mailbox.MailboxImmediateFactory;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;
import com.github.davidmoten.reels.internal.scheduler.SchedulerImmediate;
import com.github.davidmoten.reels.internal.util.Util;

/**
 * Builds an ActorRef.
 * 
 * @param <T> message type
 */
public final class ActorBuilder<T> {

    private static final AtomicLong counter = new AtomicLong();

    private final Context context;
    private final List<Matcher<T, ? extends T>> matches = new ArrayList<>();
    private Supervisor supervisor;
    private Consumer<? super Throwable> onError;
    private Scheduler scheduler;
    private String name = "Anonymous-" + counter.incrementAndGet();
    private ActorRef<?> parent; // nullable
    private Optional<Supplier<? extends Actor<T>>> factory = Optional.empty();
    private Consumer<? super ActorRef<T>> onStop = null;
    private Consumer<? super ActorRef<T>> preStart = null;
    private MailboxFactory mailboxFactory;

    ActorBuilder(Context context) {
        this.context = context;
        this.parent = context.root;
    }

    public <S extends T> ActorBuilder<T> match(Class<S> matchClass, Consumer<? super Message<T>> consumer) {
        Preconditions.checkArgumentNonNull(matchClass, "matchClass");
        Preconditions.checkArgumentNonNull(consumer, "consumer");
        Preconditions.checkArgument(!factory.isPresent(), "cannot set both matches and factory in builder");
        matches.add(new Matcher<T, S>(matchClass, null, consumer));
        return this;
    }

    public <S extends T> ActorBuilder<T> matchEquals(T value, Consumer<? super Message<T>> consumer) {
        Preconditions.checkArgumentNonNull(value, "value");
        Preconditions.checkArgumentNonNull(consumer, "consumer");
        Preconditions.checkArgument(!factory.isPresent(), "cannot set both matches and factory in builder");
        matches.add(new Matcher<T, S>(null, value, consumer));
        return this;
    }

    public ActorBuilder<T> actorClass(Class<? extends Actor<T>> actorClass, Object... args) {
        Preconditions.checkArgumentNonNull(actorClass, "actorClass");
        Preconditions.checkArgument(matches.isEmpty(), "cannot set both matches and actorClass in builder");
        return actorFactory(() -> Context.createActorObject(actorClass, args));
    }

    public ActorBuilder<T> actorFactory(Supplier<? extends Actor<T>> factory) {
        Preconditions.checkArgumentNonNull(factory, "factory");
        Preconditions.checkArgument(matches.isEmpty(), "cannot set both matches and factory in builder");
        this.factory = Optional.of(factory);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ActorBuilder<T> matchAny(Consumer<? super Message<T>> consumer) {
        Preconditions.checkArgumentNonNull(consumer, "consumer");
        Preconditions.checkArgument(!factory.isPresent(), "cannot set both matches and factory in builder");
        return match((Class<T>) Object.class, consumer);
    }

    /**
     * Sets the scheduler on which processing of messages for this Actor will be
     * scheduled. The default scheduler is {@link SchedulerForkJoinPool#INSTANCE}.
     * 
     * @param scheduler
     * @return builder
     */
    public ActorBuilder<T> scheduler(Scheduler scheduler) {
        Preconditions.checkArgumentNonNull(scheduler, "scheduler");
        this.scheduler = scheduler;
        return this;
    }

    /**
     * Sets the supervisor to be used for this Actor and for its child Actors (if
     * another supervisor not specified).
     * 
     * @param supervisor supervisor to use
     * @return builder
     */
    public ActorBuilder<T> supervisor(Supervisor supervisor) {
        Preconditions.checkArgumentNonNull(supervisor, "supervisor");
        this.supervisor = supervisor;
        return this;
    }

    /**
     * Sets unique name of the actor (should be unique amongst sibling Actors).
     * 
     * @param name unique name of the actor (should be unique amongst sibling
     *             Actors).
     * @return builder
     */
    public ActorBuilder<T> name(String name) {
        Preconditions.checkArgumentNonNull(name, "name");
        this.name = name;
        return this;
    }

    /**
     * Define what to do if one of the matchers throws.
     * 
     * @param onError what to do if one of the matchers throws.
     * @return builder
     */
    public ActorBuilder<T> onError(Consumer<? super Throwable> onError) {
        Preconditions.checkArgumentNonNull(onError, "onError");
        this.onError = onError;
        return this;
    }

    public ActorBuilder<T> preStart(Consumer<? super ActorRef<T>> preStart) {
        Preconditions.checkArgumentNonNull(preStart, "preStart");
        this.preStart = preStart;
        return this;
    }

    public ActorBuilder<T> onStop(Consumer<? super ActorRef<T>> onStop) {
        Preconditions.checkArgumentNonNull(onStop, "onStop");
        this.onStop = onStop;
        return this;
    }

    public ActorBuilder<T> parent(ActorRef<?> parent) {
        Preconditions.checkArgumentNonNull(parent, "parent");
        this.parent = parent;
        return this;
    }

    public ActorBuilder<T> mailboxFactory(MailboxFactory mailboxFactory) {
        Preconditions.checkArgumentNonNull(mailboxFactory, "mailboxFactory");
        this.mailboxFactory = mailboxFactory;
        return this;
    }

    public ActorBuilder<T> mailbox(Mailbox<T> mailbox) {
        Preconditions.checkArgumentNonNull(mailbox, "mailbox");
        return mailboxFactory(new MailboxFactory() {

            @SuppressWarnings("unchecked")
            @Override
            public <S> Mailbox<S> create() {
                return (Mailbox<S>) mailbox;
            }

        });
    }

    public ActorRef<T> build() {
        if (supervisor == null) {
            supervisor = ((ActorRefImpl<?>) parent).supervisor();
        }
        if (scheduler == null) {
            scheduler = parent.scheduler();
        }
        if (mailboxFactory == null) {
            if (scheduler instanceof SchedulerImmediate) {
                mailboxFactory = MailboxImmediateFactory.INSTANCE;
            } else {
                mailboxFactory = context.mailboxFactory();
            }
        }
        Supplier<? extends Actor<T>> f = factory.orElse(() -> new MatchingActor<T>(matches, onError, preStart, onStop));
        return context.createActor(f, name, scheduler, supervisor, parent, mailboxFactory);
    }

    private static final class Matcher<T, S extends T> {
        final Class<S> matchClass;
        final Object matchEquals;
        final Consumer<? super Message<T>> consumer;

        Matcher(Class<S> matchClass, Object matchEquals, Consumer<? super Message<T>> consumer) {
            this.matchClass = matchClass;
            this.matchEquals = matchEquals;
            this.consumer = consumer;
        }
    }

    private static final class MatchingActor<T> implements Actor<T> {

        private final List<Matcher<T, ? extends T>> matchers;
        private final Consumer<? super Throwable> onError;
        private final Consumer<? super ActorRef<T>> preStart;
        private final Consumer<? super ActorRef<T>> onStop;

        public MatchingActor(List<Matcher<T, ? extends T>> matchers, Consumer<? super Throwable> onError,
                Consumer<? super ActorRef<T>> preStart, Consumer<? super ActorRef<T>> onStop) {
            this.matchers = matchers;
            this.onError = onError;
            this.preStart = preStart;
            this.onStop = onStop;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onMessage(Message<T> message) {
            for (Matcher<T, ? extends T> matcher : matchers) {
                if (matcher.matchEquals != null && matcher.matchEquals.equals(message.content())
                        || matcher.matchEquals == null && matcher.matchClass.isInstance(message.content())) {
                    try {
                        ((Matcher<T, T>) matcher).consumer.accept(message);
                    } catch (Throwable e) {
                        if (onError != null) {
                            onError.accept(e);
                        } else {
                            Util.rethrow(e);
                        }
                    }
                    return;
                }
            }
        }

        @Override
        public void preStart(ActorRef<T> self) {
            if (preStart != null) {
                preStart.accept(self);
            }
        }

        @Override
        public void onStop(ActorRef<T> self) {
            if (onStop != null) {
                onStop.accept(self);
            }
        }
    }

}
