package com.github.davidmoten.reels;

public interface Actor<T> {
    
    void onMessage(MessageContext<T> context, T message);
    
    public static <T> ActorBuilder<T> builder(Class<T> messageClass) {
        return new ActorBuilder<T>();
    }
    
    
}
