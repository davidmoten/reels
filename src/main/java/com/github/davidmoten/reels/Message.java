package com.github.davidmoten.reels;

public final class Message<T> {

    private final T content;
    private final ActorRef<?> sender; // nullable
    private final ActorRef<T> recipient;

    /**
     * Constructor. Not part of public API.
     * 
     * @param content   message content
     * @param recipient message receiver
     * @param sender    message sender
     */
    public Message(T content, ActorRef<T> recipient, ActorRef<?> sender) {
        this.content = content;
        this.recipient = recipient;
        this.sender = sender;
    }

    public T content() {
        return content;
    }

    public ActorRef<T> self() {
        return recipient;
    }

    public ActorRef<T> recipient() {
        return recipient;
    }

    @SuppressWarnings("unchecked")
    public <S> ActorRef<S> sender() {
        return (ActorRef<S>) sender;
    }

    public Context context() {
        return recipient.context();
    }

    @Override
    public String toString() {
        return "Message[" + content + ", sender=" + sender + ", recipient=" + recipient + "]";
    }
}
