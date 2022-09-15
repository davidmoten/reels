package com.github.davidmoten.reels;

public interface SupervisedActorRef<T> extends ActorRef<T> {

    /**
     * Recreates the Actor object that processes messages. The message queue is
     * untouched. Must be called synchronously to avoid undesired race conditions
     * (don't schedule a restart for instance).
     */
    void restart();

    /**
     * Clears the queue of messages waiting to be processed by the Actor. Must be
     * called synchronously to avoid undesired race conditions.
     * 
     */
    void clearQueue();

}
