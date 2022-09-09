package com.github.davidmoten.reels.internal;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.MessageContext;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Supervisor;
import com.github.davidmoten.reels.internal.queue.MpscLinkedQueue;
import com.github.davidmoten.reels.internal.queue.SimplePlainQueue;

public final class ActorRefImpl<T> implements ActorRef<T>, Runnable, Disposable {

    private final Actor<T> actor;
    private final SimplePlainQueue<Message<T>> queue;
    private final Scheduler scheduler;
    private final Context context;
    private final Supervisor supervisor;
    private final AtomicInteger wip = new AtomicInteger();
    private volatile boolean disposed;

    public ActorRefImpl(Actor<T> actor, Scheduler scheduler, Context context, Supervisor supervisor) {
        this.actor = actor;
        this.scheduler = scheduler;
        this.context = context;
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
        tell(message, null);
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        if (disposed) {
            return;
        }
        queue.offer(new Message<T>(message, Optional.ofNullable(sender)));
        scheduler.schedule(this);
    }

    @Override
    public void run() {
        if (wip.getAndIncrement() == 0) {
            int missed = 1;
            Message<T> message;
            while ((message = queue.poll()) != null) {
                if (disposed) {
                    return;
                }
                try {
                    actor.onMessage(new MessageContext<T>(this, message.sender()), message.content());
                } catch (Throwable e) {
                    supervisor.processFailure(context, this, e);
                }
            }
            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                return;
            }
        }
    }

}
