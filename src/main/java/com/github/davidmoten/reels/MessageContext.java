package com.github.davidmoten.reels;

import java.util.Optional;

public final class MessageContext<T> {

    private final ActorRef<T> self;
    private final Optional<ActorRef<?>> sender;

    public MessageContext(ActorRef<T> self, Optional<ActorRef<?>> sender) {
        this.self = self;
        this.sender = sender;
    }

    public ActorRef<T> self() {
        return self;
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<ActorRef<S>> sender() {
        return (Optional<ActorRef<S>>) (Optional<?>) sender;
    }

}
