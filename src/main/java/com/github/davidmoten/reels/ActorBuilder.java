package com.github.davidmoten.reels;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.Preconditions;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;
import com.github.davidmoten.reels.internal.util.Util;

public final class ActorBuilder<T> {

    private static final AtomicLong counter = new AtomicLong();

    private final Context context;
    private final List<Matcher<T, ? extends T>> matches = new ArrayList<>();
    private Supervisor supervisor;
    private Consumer<? super Throwable> onError;
    private Scheduler scheduler = Scheduler.forkJoin();
    private String name = "Anonymous-" + counter.incrementAndGet();
    private ActorRef<?> parent; // nullable
    private Optional<Supplier<? extends Actor<T>>> factory = Optional.empty();
    private Consumer<? super Context> onStop = null;

    ActorBuilder(Context context) {
        this.context = context;
        this.parent = context.root;
    }

    public <S extends T> ActorBuilder<T> match(Class<S> matchClass, Consumer<? super Message<T>> consumer) {
        Preconditions.checkArgument(!factory.isPresent(), "cannot set both matches and factory in builder");
        matches.add(new Matcher<T, S>(matchClass, null, consumer));
        return this;
    }

    public <S extends T> ActorBuilder<T> matchEquals(T value, Consumer<? super Message<T>> consumer) {
        Preconditions.checkArgument(!factory.isPresent(), "cannot set both matches and factory in builder");
        matches.add(new Matcher<T, S>(null, value, consumer));
        return this;
    }

    public ActorBuilder<T> factory(Supplier<? extends Actor<T>> factory) {
        Preconditions.checkArgument(matches.isEmpty(), "cannot set both matches and factory in builder");
        this.factory = Optional.of(factory);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ActorBuilder<T> matchAll(Consumer<? super Message<T>> consumer) {
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

    public ActorBuilder<T> supervisor(Supervisor supervisor) {
        Preconditions.checkArgumentNonNull(supervisor, "supervisor");
        this.supervisor = supervisor;
        return this;
    }

    public ActorBuilder<T> name(String name) {
        Preconditions.checkArgumentNonNull(name, "name");
        this.name = name;
        return this;
    }

    public ActorBuilder<T> onError(Consumer<? super Throwable> onError) {
        Preconditions.checkArgumentNonNull(onError, "onError");
        this.onError = onError;
        return this;
    }

    public ActorBuilder<T> onStop(Consumer<? super Context> onStop) {
        Preconditions.checkArgumentNonNull(onStop, "onStop");
        this.onStop = onStop;
        return this;
    }

    public ActorBuilder<T> parent(ActorRef<?> parent) {
        Preconditions.checkArgumentNonNull(parent, "parent");
        this.parent = parent;
        return this;
    }

    public ActorRef<T> build() {
        if (supervisor == null) {
            supervisor = ((ActorRefImpl<?>) parent).supervisor();
        }
        Supplier<? extends Actor<T>> f = factory.orElse(() -> new MatchingActor<T>(matches, onError, onStop));
        return context.createActor(f, name, scheduler, supervisor, parent);
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
        private final Consumer<? super Context> onStop;

        public MatchingActor(List<Matcher<T, ? extends T>> matchers, Consumer<? super Throwable> onError,
                Consumer<? super Context> onStop) {
            this.matchers = matchers;
            this.onError = onError;
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
        public void onStop(Context context) {
            if (onStop != null) {
                onStop.accept(context);
            }
        }
    }

}
