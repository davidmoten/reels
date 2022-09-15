package com.github.davidmoten.reels.internal.supervisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.SupervisedActorRef;
import com.github.davidmoten.reels.Supervisor;

public final class SupervisorDefault implements Supervisor {

    public static final SupervisorDefault INSTANCE = new SupervisorDefault();

    private static final Logger log = LoggerFactory.getLogger(SupervisorDefault.class);

    private SupervisorDefault() {
        // prevent instantiation
    }

    @Override
    public void processFailure(Context context, SupervisedActorRef<?> actorRef, Throwable error) {
        log.error(error.getMessage(), error);
        actorRef.restart();
        log.warn("actor.onMessage threw (error logged above) and was caught by SupervisorDefault. The actor '"
                + actorRef.name()
                + "'was restarted and will continue to process messages (remaining messages were untouched).");
    }

}
