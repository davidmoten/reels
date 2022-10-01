package com.github.davidmoten.reels;

import java.util.concurrent.TimeUnit;

public interface SupervisedActorRef<T> extends ActorRef<T> {

    /**
     * Recreates the Actor object that processes messages on the next message polled
     * from the queue (mailbox). Should be called synchronously from the Supervisor
     * object.
     */
    void restart();

    /**
     * Will restart the actor after the given given delay on the next message polled
     * from the queue (mailbox). Until that time the actor is in a paused state and
     * no messages will be processed (messages will be buffered in-memory). Once
     * restarted message processing resumes.
     * 
     * @param delay time till restart called
     * @param unit  duration unit
     */
    void restart(long delay, TimeUnit unit);

}
