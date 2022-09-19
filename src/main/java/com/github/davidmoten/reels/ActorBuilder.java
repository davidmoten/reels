package com.github.davidmoten.reels;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.davidmoten.reels.internal.Preconditions;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;
import com.github.davidmoten.reels.internal.util.Util;

public final class ActorBuilder<T> {

    private final Context context;
    private final List<Matcher<T, ? extends T>> matches = new ArrayList<>();
    private Supervisor supervisor;
    private Consumer<? super Throwable> onError;
    private Scheduler scheduler = Scheduler.forkJoin();
    private String name = UUID.randomUUID().toString().replace("-", "");
    private Optional<ActorRef<?>> parent = Optional.empty();
    private Optional<Supplier<? extends Actor<T>>> factory = Optional.empty();

    ActorBuilder(Context context) {
        this.context = context;
        this.supervisor = context.supervisor();
    }

    public <S extends T> ActorBuilder<T> match(Class<S> matchClass, BiConsumer<MessageContext<T>, ? super S> consumer) {
        Preconditions.checkArgument(!factory.isPresent(), "cannot set both matches and factory in builder");
        matches.add(new Matcher<T, S>(matchClass, consumer));
        return this;
    }

    public ActorBuilder<T> factory(Supplier<? extends Actor<T>> factory) {
        Preconditions.checkArgument(matches.isEmpty(), "cannot set both matches and factory in builder");
        this.factory = Optional.of(factory);
        return this;
    }

    @SuppressWarnings("unchecked")
    public ActorBuilder<T> matchAll(BiConsumer<MessageContext<T>, ? super T> consumer) {
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
        Preconditions.checkParameterNotNull(scheduler, "scheduler");
        this.scheduler = scheduler;
        return this;
    }

    public ActorBuilder<T> supervisor(Supervisor supervisor) {
        Preconditions.checkParameterNotNull(supervisor, "supervisor");
        this.supervisor = supervisor;
        return this;
    }

    public ActorBuilder<T> name(String name) {
        Preconditions.checkParameterNotNull(name, "name");
        this.name = name;
        return this;
    }

    public ActorBuilder<T> onError(Consumer<? super Throwable> onError) {
        Preconditions.checkParameterNotNull(onError, "onError");
        this.onError = onError;
        return this;
    }

    public ActorBuilder<T> parent(ActorRef<?> parent) {
        Preconditions.checkParameterNotNull(parent, "parent");
        this.parent = Optional.of(parent);
        return this;
    }

    public ActorRef<T> build() {
        Supplier<? extends Actor<T>> f = factory.orElse(() -> new MatchingActor<T>(matches, onError));
        return context.createActor(f, name, scheduler, supervisor, parent);
    }

    private static final class Matcher<T, S extends T> {
        final Class<S> matchClass;
        final BiConsumer<MessageContext<T>, ? super S> consumer;

        Matcher(Class<S> matchClass, BiConsumer<MessageContext<T>, ? super S> consumer2) {
            this.matchClass = matchClass;
            this.consumer = consumer2;
        }
    }

    private static final class MatchingActor<T> implements Actor<T> {

        private final List<Matcher<T, ? extends T>> matchers;
        private final Consumer<? super Throwable> onError;

        public MatchingActor(List<Matcher<T, ? extends T>> matchers, Consumer<? super Throwable> onError) {
            this.matchers = matchers;
            this.onError = onError;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onMessage(MessageContext<T> context, T message) {
            for (Matcher<T, ? extends T> matcher : matchers) {
                if (matcher.matchClass.isInstance(message)) {
                    try {
                        ((Matcher<T, T>) matcher).consumer.accept((MessageContext<T>) context, message);
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
    }

}
