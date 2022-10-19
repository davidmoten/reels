package com.github.davidmoten.reels.internal;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Supervisor;

public class ActorRefSerialized<T> extends ActorRefImpl<T> {

    private final AtomicInteger wip = new AtomicInteger();

    protected ActorRefSerialized(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler,
            Context context, Supervisor supervisor, ActorRef<?> parent) {
        super(name, factory, scheduler, context, supervisor, parent);
    }

    @Override
    public void run() {
        if (wip.getAndIncrement() == 0) {
            while (true) {
                int missed = 1;
                drain();
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
    }

}
