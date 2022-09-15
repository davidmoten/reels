package com.github.davidmoten.reels;

public interface ActorRef<T> extends Disposable {

    /**
     * Sends the message without a sender.
     * 
     * @param message
     */
    void tell(T message);

    /**
     * Sends the message with the given sender.
     * 
     * @param message message to send
     * @param sender  message sender (for replies as an example)
     */
    void tell(T message, ActorRef<?> sender);

    /**
     * Sends a Poision Pill message to the actor which will be disposed when that
     * message is processed. The Poison Pill message does not jump the queue past
     * other already waiting messages on the Actor.
     */
    void stop();

    /**
     * Returns the current actor system context.
     * 
     * @return actor system context
     */
    Context context();

    /**
     * The unique Actor identifier for the current Context.
     * 
     * @return unique identifier for the current Context
     */
    String name();

    /**
     * Returns the worker that the Actor's messages are processed on.
     * 
     * @return the worker that the Actor's messages are processed on
     */
    Worker worker();

}
