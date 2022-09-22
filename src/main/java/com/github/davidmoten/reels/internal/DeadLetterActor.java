package com.github.davidmoten.reels.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.MessageContext;

public final class DeadLetterActor implements Actor<Object> {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterActor.class);

    @Override
    public void onMessage(Message<Object> message) {
        log.info("dead letter message={}", message.content());
    }

    @Override
    public void onStop(MessageContext<Object> context) {
        // TODO

    }

}