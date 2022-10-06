package com.github.davidmoten.reels.internal;

import java.util.function.Supplier;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Supervisor;

public class ActorRefNotSerialized<T> extends ActorRefImpl<T> {

    boolean running;

    protected ActorRefNotSerialized(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler,
            Context context, Supervisor supervisor, ActorRef<?> parent) {
        super(name, factory, scheduler, context, supervisor, parent);
    }
    
    @Override
    public void run() {
        if (!running) {
            running = true;
            drain();
            running = false;
        }
    }
    
}
