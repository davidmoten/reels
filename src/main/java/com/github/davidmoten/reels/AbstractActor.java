package com.github.davidmoten.reels;

/**
 * Convenience class as most of the time we will just implement onMessage.
 * 
 * @param <T> message type
 */
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
