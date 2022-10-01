package com.github.davidmoten.reels;

import java.util.concurrent.TimeUnit;

public interface SupervisedActorRef<T> extends ActorRef<T> {

    /**
     * Recreates the Actor object that processes messages on the next message polled
     * from the queue (mailbox). Should be called synchronously from the Supervisor
     * object.
     * 
     * @return true if and only if command accepted (for example if actor disposed
     *         or stopped then won't be accepted)
     */
    boolean restart();

    /**
     * Will restart the actor after the given given delay on the next message polled
     * from the queue (mailbox). Until that time the actor is in a paused state and
     * no messages will be processed (messages will be buffered in-memory). Once
     * restarted message processing resumes.
     * 
     * @param delay time till restart called
     * @param unit  duration unit
     * @return true if and only if command accepted (for example if actor disposed
     *         or stopped then won't be accepted)
     */
    boolean restart(long delay, TimeUnit unit);

    /**
     * Pauses message processing for the given duration.
     * 
     * @param duration duration of pause
     * @param unit     duration unit
     * @return true if and only if command accepted (for example if actor disposed
     *         or stopped then won't be accepted)
     */
    boolean pause(long duration, TimeUnit unit);

    /**
     * Ensures that the message that prompted failure is retried (for example with a
     * delay and a restarted actor if you also call
     * {@link SupervisedActorRef#restart(long, TimeUnit)}).
     */
    void retry();

}
