package com.github.davidmoten.reels.internal;

import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Supervisor;

public final class ActorRefImpl<T> implements ActorRef<T>, Runnable, Disposable {

    private final SimplePlainQueue<Message<T>> queue;
    private final Scheduler scheduler;
    private final Supervisor supervisor;
    private volatile boolean disposed;

    public ActorRefImpl(Scheduler scheduler, Supervisor supervisor) {
        this.scheduler = scheduler;
        this.supervisor = supervisor;
        this.queue = new MpscLinkedQueue<Message<T>>();
    }

    @Override
    public void dispose() {
        this.disposed = true;
        queue.clear();
    }

    @Override
    public void tell(T message) {
        queue.offer(new Message<T>(message, null));
        scheduler.schedule(this);
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        Preconditions.checkNotNull(sender, "sender cannot be null");
        queue.offer(new Message<T>(message, sender));
        scheduler.schedule(this);
    }

    @Override
    public void run() {
        // TODO write drain method
    }

}
