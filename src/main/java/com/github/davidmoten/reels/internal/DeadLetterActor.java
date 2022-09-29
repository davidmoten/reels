package com.github.davidmoten.reels.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.AbstractActor;
import com.github.davidmoten.reels.Message;

public final class DeadLetterActor extends  AbstractActor<Object> {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterActor.class);

    @Override
    public void onMessage(Message<Object> message) {
        log.info("dead letter message={}", message.content());
    }

}
