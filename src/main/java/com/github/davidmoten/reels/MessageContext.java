package com.github.davidmoten.reels;

import java.util.Optional;

public final class MessageContext<T> {

    private final ActorRef<T> self;
    private final ActorRef<?> sender; // nullable but don't use Optional to reduce allocation pressure

    /**
     * Beware, this constructor not part of the public API.
     * 
     * @param self
     * @param sender
     */
    public MessageContext(ActorRef<T> self, ActorRef<?> sender) {
        this.self = self;
        this.sender = sender;
    }

    public ActorRef<T> self() {
        return self;
    }

    @SuppressWarnings("unchecked")
    public <S> Optional<ActorRef<S>> sender() {
        return Optional.ofNullable((ActorRef<S>) sender);
    }

    public Context context() {
        return self.context();
    }

}
