package com.github.davidmoten.reels.internal;

import java.util.Optional;

import com.github.davidmoten.reels.ActorRef;

public class Message<T> {

    private final T content;
    private final Optional<ActorRef<?>> sender;

    public Message(T content, Optional<ActorRef<?>> sender) {
        this.content = content;
        this.sender = sender;
    }

    public T content() {
        return content;
    }

    public Optional<ActorRef<?>> sender() {
        return sender;
    }
}
