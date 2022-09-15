package com.github.davidmoten.reels;

public interface SupervisedActorRef<T> extends ActorRef<T> {

    /**
     * Recreates the Actor object that processes messages. The message queue is
     * untouched.
     */
    void restart();

    /**
     * Clears the queue of messages waiting to be processed by the Actor.
     */
    void clearQueue();

}
