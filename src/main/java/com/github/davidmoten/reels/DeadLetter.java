package com.github.davidmoten.reels;

public final class DeadLetter {
    
    private final Message<?> message;

    public DeadLetter(Message<?> message) {
        this.message = message;
    }
    
    public Message<?> message() {
        return message;
    }

    @Override
    public String toString() {
        return "DeadLetter [message=" + message + "]";
    }
}
