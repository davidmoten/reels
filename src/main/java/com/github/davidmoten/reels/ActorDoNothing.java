package com.github.davidmoten.reels;

public final class ActorDoNothing<T> extends AbstractActor<T> {
    
    private static final ActorDoNothing<Object> INSTANCE = new ActorDoNothing<>(); 
    
    @SuppressWarnings("unchecked")
    public static <T> ActorDoNothing<T> create() {
        return (ActorDoNothing<T>) INSTANCE;
    }

    private ActorDoNothing() {
        // prevent instantiation
    }
    
    @Override
    public void onMessage(Message<T> message) {
        // do nothing
    }

}
