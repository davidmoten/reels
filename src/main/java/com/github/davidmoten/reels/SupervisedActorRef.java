package com.github.davidmoten.reels;

public interface SupervisedActorRef<T> extends ActorRef<T> {

    /**
     * Recreates the Actor object that processes messages. The message queue is
     * untouched. Must be called synchronously to avoid undesired race conditions
     * (don't schedule a restart for instance). Note that a restart <b>will call dispose 
     * on all children</b>.
     */
    void restart(boolean disposeChildren);

}
