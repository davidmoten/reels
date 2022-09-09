package com.github.davidmoten.reels;

public interface Supervisor {
    
    void processFailure(Context context, ActorRef<?> actorRef, Throwable error);

}
