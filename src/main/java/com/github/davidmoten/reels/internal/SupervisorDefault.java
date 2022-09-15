package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.SupervisedActorRef;
import com.github.davidmoten.reels.Supervisor;

public final class SupervisorDefault implements Supervisor {

    public static final SupervisorDefault INSTANCE = new SupervisorDefault();

    private SupervisorDefault() {
        // prevent instantiation
    }

    @Override
    public void processFailure(Context context, SupervisedActorRef<?> actorRef, Throwable error) {
        error.printStackTrace();
        actorRef.dispose();
        System.out.println(
                "actor.onMessage threw and was caught by SupervisorDefault. The actor was disposed and will not process further messages.");
    }

}
