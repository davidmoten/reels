package com.github.davidmoten.reels;

public abstract class AbstractActor<T> implements Actor<T> {
    
    @Override
    public void preStart(ActorRef<T> self) {
        // do nothing
    }

    @Override
    public void onStop(ActorRef<T> self) {
        // do nothing
    }
    
}
