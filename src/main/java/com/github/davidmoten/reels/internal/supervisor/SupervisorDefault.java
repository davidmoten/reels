package com.github.davidmoten.reels.internal.supervisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.SupervisedActorRef;
import com.github.davidmoten.reels.Supervisor;

public final class SupervisorDefault implements Supervisor {

    public static final SupervisorDefault INSTANCE = new SupervisorDefault();

    private static final Logger log = LoggerFactory.getLogger(SupervisorDefault.class);

    private SupervisorDefault() {
        // prevent instantiation
    }

    @Override
    public void processFailure(Message<?> message, SupervisedActorRef<?> self, Throwable error) {
        log.error(error.getMessage(), error);
        self.dispose();
        log.warn("actor.onMessage threw (error logged above) and was caught by " + this.getClass().getSimpleName()
                + ". The actor '" + self.name()
                + "' was disposed thus pending messages have been cleared and the actor will no longer accept messages.");
    }

}
