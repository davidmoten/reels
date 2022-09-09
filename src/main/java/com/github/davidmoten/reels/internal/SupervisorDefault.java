package com.github.davidmoten.reels.internal;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Supervisor;

public final class SupervisorDefault implements Supervisor{

    public static final SupervisorDefault INSTANCE = new SupervisorDefault();
    
    private SupervisorDefault() {
        // prevent instantiation
    }
    
    @Override
    public void processFailure(Context context, ActorRef<?> actorRef, Throwable error) {
        error.printStackTrace();
        actorRef.dispose();
    }

}
