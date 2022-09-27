package com.github.davidmoten.reels;

import java.util.Optional;

public class Message<T> implements MessageContext<T> {

    private final T content;
    private final ActorRef<?> sender; // nullable
    private final ActorRef<T> self;

    /**
     * Constructor. Not part of public API.
     * 
     * @param content message content
     * @param self    message receiver
     * @param sender  message sender
     */
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

    /**
     * Returns the sender {@link ActorRef}. May be null. This method exists to
     * reduce allocation pressue from Optional creation.
     * 
     * @param <S> message type
     * @return actor reference
     */
    @SuppressWarnings("unchecked")
    public <S> ActorRef<S> senderRaw() {
        return (ActorRef<S>) sender;
    }

    @Override
    public Context context() {
        return self.context();
    }

    @Override
    public String toString() {
        return "Message[" + content + "]";
    }
}
