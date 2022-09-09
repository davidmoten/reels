package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.ActorRef;

public class Message<T> {

    private final T content;
    private final ActorRef<?> sender;

    public Message(T content, ActorRef<?> sender) {
        this.content = content;
        this.sender = sender;
    }

    public T content() {
        return content;
    }

    public ActorRef<?> sender() {
        return sender;
    }
}
