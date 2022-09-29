package com.github.davidmoten.reels;

public class ActorDoNothing<T> extends AbstractActor<T> {

    @Override
    public void onMessage(Message<T> message) {
        // do nothing
    }

}
