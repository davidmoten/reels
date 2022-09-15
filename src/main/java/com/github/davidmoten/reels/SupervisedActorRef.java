package com.github.davidmoten.reels;

public interface SupervisedActorRef<T> extends ActorRef<T>{

    void restart();
    
    void clearQueue();
    
}
