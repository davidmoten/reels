package com.github.davidmoten.reels;

public interface Supervisor {

    /**
     * Processes a thrown error from Actor.onMessage. This method must not throw.
     * 
     * <p>
     * Note that because the method is provided with the actorRef that the dispose
     * method can be called on the actorRef which will stop all further processing
     * of messages including already queued ones. If the actorRef is not disposed
     * then message processing will continue with following messages as normal.
     * 
     * @param context  context the actor
     * @param actorRef reference to the actor where the error occurred
     * @param error    the error throw in the Actor.onMessage method
     */
    void processFailure(Context context, ActorRef<?> actorRef, Throwable error);

}
