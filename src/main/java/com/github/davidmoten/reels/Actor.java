package com.github.davidmoten.reels;

public interface Actor<T> {
    
    void onMessage(MessageContext<T> context, T message);
    
}
