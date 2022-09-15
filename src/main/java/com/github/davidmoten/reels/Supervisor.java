package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.supervisor.SupervisorDefault;

@FunctionalInterface
public interface Supervisor {

    final static Supervisor DEFAULT = SupervisorDefault.INSTANCE;

    /**
     * Processes a thrown error from Actor.onMessage. This method <b>must not
     * throw</b>.
     * 
     * <p>
     * Note that because the method is provided with the actorRef that the dispose
     * method can be called on the actorRef which will stop all further processing
     * of messages including already queued ones. If the actorRef is not disposed
     * then message processing will continue with subsequent messages as normal.
     * 
     * <p>
     * A {@link SupervisedActorRef} also has methods to restart the actor (recreate
     * the actor object which discards current state in the actor) but retain the
     * queued messages, and also the ability to clear the current message queue.
     * 
     * @param context  context the actor
     * @param actorRef reference to the actor where the error occurred
     * @param error    the error throw in the Actor.onMessage method
     */
    void processFailure(Context context, SupervisedActorRef<?> actorRef, Throwable error);

}
