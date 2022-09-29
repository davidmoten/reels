package com.github.davidmoten.reels;

public class ActorDoNothing<T> implements Actor<T> {

    @Override
    public void onMessage(Message<T> message) {
        // do nothing
    }

    @Override
    public void onStop(MessageContext<T> context) {
        // do nothing
    }

}
