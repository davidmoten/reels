package com.github.davidmoten.reels;

import java.util.concurrent.TimeUnit;

public interface SupervisedActorRef<T> extends ActorRef<T> {

    /**
     * Recreates the Actor object that processes messages on the next message polled
     * from the queue (mailbox).
     */
    void restart();

    /**
     * TODO
     * 
     * @param duration time till restart called
     * @param unit     duration unit
     */
    void restart(long duration, TimeUnit unit);

}
