package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.MessageContext;

public final class DeadLetterActor implements Actor<Object> {

    @Override
    public void onMessage(MessageContext<Object> context, Object message) {
        // TODO
    }

    @Override
    public void onStop(MessageContext<Object> context) {
        // TODO

    }

}
