package com.github.davidmoten.reels.internal;

import java.util.Optional;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.MessageContext;

public class Message<T> implements MessageContext<T> {

    private final T content;
    private final ActorRef<?> sender; // nullable
    private final ActorRef<T> self;

    public Message(T content, ActorRef<T> self, ActorRef<?> sender) {
        this.content = content;
        this.self = self;
        this.sender = sender;
    }

    public T content() {
        return content;
    }

    public ActorRef<T> self() {
        return self;
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<ActorRef<S>> sender() {
        return Optional.ofNullable((ActorRef<S>) sender);
    }

    public ActorRef<?> senderRaw() {
        return sender;
    }

    @Override
    public Context context() {
        return self.context();
    }
}
