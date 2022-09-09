package com.github.davidmoten.reels;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.github.davidmoten.reels.internal.ActorRefImpl;
import com.github.davidmoten.reels.internal.SupervisorDefault;

public final class ActorBuilder<T> {

    private final Context context;
    private final List<Matcher<? extends T>> matches = new ArrayList<>();
    private Consumer<? super Throwable> onError;
    private Scheduler scheduler = Scheduler.computation();
    private Supervisor supervisor = SupervisorDefault.INSTANCE;

    ActorBuilder(Context context) {
        this.context = context;
    }

    public <S extends T> ActorBuilder<T> match(Class<S> matchClass, BiConsumer<MessageContext<S>, S> consumer) {
        matches.add(new Matcher<S>(matchClass, consumer));
        return this;
    }

    public ActorBuilder<T> scheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }

    public ActorBuilder<T> supervisor(Supervisor supervisor) {
        this.supervisor = supervisor;
        return this;
    }

    public ActorBuilder<T> onError(Consumer<? super Throwable> onError) {
        this.onError = onError;
        return this;
    }

    public ActorRef<T> build() {
        return new ActorRefImpl<T>(new MatchingActor<T>(matches, onError), scheduler, context, supervisor);
    }

    private static final class Matcher<S> {
        final Class<S> matchClass;
        final BiConsumer<MessageContext<S>, S> consumer;

        Matcher(Class<S> matchClass, BiConsumer<MessageContext<S>, S> consumer) {
            this.matchClass = matchClass;
            this.consumer = consumer;
        }
    }

    private static final class MatchingActor<T> implements Actor<T> {

        private final List<Matcher<? extends T>> matchers;
        private final Consumer<? super Throwable> onError;

        public MatchingActor(List<Matcher<? extends T>> matchers, Consumer<? super Throwable> onError) {
            this.matchers = matchers;
            this.onError = onError;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onMessage(MessageContext<T> context, T message) {
            for (Matcher<?> matcher : matchers) {
                if (matcher.matchClass.isInstance(message)) {
                    try {
                        ((Matcher<Object>) matcher).consumer.accept((MessageContext<Object>) context, message);
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
