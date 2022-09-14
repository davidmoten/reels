package com.github.davidmoten.reels.internal;

import java.util.concurrent.atomic.AtomicInteger;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.MessageContext;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.Supervisor;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.queue.MpscLinkedQueue;
import com.github.davidmoten.reels.internal.queue.SimplePlainQueue;

public final class ActorRefImpl<T> implements ActorRef<T>, Runnable, Disposable {

//    private static final Logger log = LoggerFactory.getLogger(ActorRefImpl.class);

    private static final Object POISON_PILL = new Object();

    private final String name;
    private final Actor<T> actor;
    private final SimplePlainQueue<Message<T>> queue;
    private final Context context;
    private final Supervisor supervisor;
    private final AtomicInteger wip = new AtomicInteger();
    private volatile boolean disposed;
    private final Worker worker;

    public ActorRefImpl(String name, Actor<T> actor, Scheduler scheduler, Context context, Supervisor supervisor) {
        this.name = name;
        this.actor = actor;
        this.context = context;
        this.supervisor = supervisor;
        this.queue = new MpscLinkedQueue<Message<T>>();
        this.worker = scheduler.createWorker();
    }

    @Override
    public void dispose() {
        this.disposed = true;
        queue.clear();
        context.disposeActor(name);
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
        queue.offer(new Message<T>(message, sender));
        worker.schedule(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void kill() {
        tell((T) POISON_PILL);
    }

    @Override
    public void run() {
        // drain queue
        if (wip.getAndIncrement() == 0) {
//            log.info("starting drain");
            int missed = 1;
            Message<T> message;
            while ((message = queue.poll()) != null) {
//                log.info("message polled=" + message.content());
                if (message.content() == POISON_PILL) {
                    dispose();
                    return;
                } else if (disposed) {
                    queue.clear();
                    return;
                }
                try {
//                    log.info("calling onMessage");
                    actor.onMessage(new MessageContext<T>(this, message.sender()), message.content());
//                    log.info("called onMessage");
                } catch (Throwable e) {
                    // if the line below throws then the actor will no longer process messages
                    // (because wip will be != 0)
                    supervisor.processFailure(context, this, e);
                }
            }
            missed = wip.addAndGet(-missed);
            if (missed == 0) {
                return;
            }
        }
    }
    
    @Override
    public boolean isDisposed() {
        return disposed;
    }

}
