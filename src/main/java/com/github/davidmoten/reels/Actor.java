package com.github.davidmoten.reels;

public interface Actor<T> {

    /**
     * Processes a queued message to the actor. If this method throws then the
     * Supervisor allocated to the Actor will handle it (default is to write an
     * error to stderr, clear the message queue and prevent further message
     * processing).
     * 
     * @param context gives access to self and sender {@link ActorRef}s and to
     *                overall {@link Context} (which might be used for creating more
     *                actors for example)
     * @param message message to be processed
     */
    void onMessage(Message<T> message);

    /**
     * This method called after an actor is stopped (via {@link ActorRef#stop()} or
     * when an actor is restarted.
     * 
     * @param context owning Context
     */
    void onStop(Context context);

}
