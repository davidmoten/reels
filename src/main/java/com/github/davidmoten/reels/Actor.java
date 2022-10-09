package com.github.davidmoten.reels;

public interface Actor<T> {

    /**
     * Processes a queued message to the actor. If this method throws then the
     * Supervisor allocated to the Actor will handle it (default is
     * {@code Supervisor.defaultSupervisor()}.
     * 
     * @param context gives access to self (recipient) and sender {@link ActorRef}s and to
     *                overall {@link Context} (which might be used for creating more
     *                actors for example)
     * @param message message to be processed
     */
    void onMessage(Message<T> message);

    /**
     * Called just after creating this, before processing any messages.
     * @param self ActorRef referring to this
     */
    void preStart(ActorRef<T> self);

    /**
     * This method called after an actor is stopped (via {@link ActorRef#stop()} or
     * when an actor is restarted.
     * 
     * @param self ActorRef referring to this
     */
    void onStop(ActorRef<T> self);

}
