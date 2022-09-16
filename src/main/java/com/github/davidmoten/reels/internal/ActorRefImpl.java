package com.github.davidmoten.reels.internal;

import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.MessageContext;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.SupervisedActorRef;
import com.github.davidmoten.reels.Supervisor;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.queue.MpscLinkedQueue;
import com.github.davidmoten.reels.internal.queue.SimplePlainQueue;

public final class ActorRefImpl<T> implements SupervisedActorRef<T>, Runnable, Disposable {

//    private static Logger log = LoggerFactory.getLogger(ActorRefImpl.class);

    private static final Object POISON_PILL = new Object();

    private final String name;
    private final Supplier<? extends Actor<T>> factory;
    private final SimplePlainQueue<Message<T>> queue; // mailbox
    private final Context context;
    private final Supervisor supervisor;
    private final AtomicInteger wip = new AtomicInteger();
    private final Scheduler scheduler;
    private final Worker worker;
    private final Optional<ActorRef<?>> parent;
    private Actor<T> actor; // mutable because recreated if restart called
    private final ConcurrentHashMap<ActorRef<?>, ActorRef<?>> children;
    private volatile boolean disposed;

    public static <T> ActorRefImpl<T> create(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler,
            Context context, Supervisor supervisor, Optional<ActorRef<?>> parent) {
        ActorRefImpl<T> a = new ActorRefImpl<T>(name, factory, scheduler, context, supervisor, parent);
        parent.ifPresent(p -> ((ActorRefImpl<?>) p).addChild(a));
        return a;
    }

    private ActorRefImpl(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler, Context context,
            Supervisor supervisor, Optional<ActorRef<?>> parent) {
        this.name = name;
        this.factory = factory;
        this.context = context;
        this.supervisor = supervisor;
        this.queue = new MpscLinkedQueue<Message<T>>();
        this.worker = scheduler.createWorker();
        this.parent = parent;
        this.actor = factory.get();
        this.scheduler = scheduler;
        this.children = new ConcurrentHashMap<ActorRef<?>, ActorRef<?>>();
    }

    void addChild(ActorRef<?> actor) {
        children.put(actor, actor);
    }

    void removeChild(ActorRef<?> actor) {
        children.remove(actor);
    }

    @Override
    public void dispose() {
        if (!disposed) {
            worker.dispose();
            queue.clear();
            parent.ifPresent(p -> ((ActorRefImpl<?>) p).removeChild(this));
            context.removeActor(name);
        }
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
    public void stop() {
        tell((T) POISON_PILL);
    }

    @Override
    public void run() {
        // drain queue
//        info("run called");
        if (wip.getAndIncrement() == 0) {
//            info("starting drain");
            while (true) {
                int missed = 1;
                Message<T> message;
                while ((message = queue.poll()) != null) {
//                    info("message polled=" + message.content());
                    if (message.content() == POISON_PILL) {
                        Enumeration<ActorRef<?>> en = children.keys();
                        while (en.hasMoreElements()) {
                            ActorRef<?> child = en.nextElement();
                            child.stop();
                        }
                        dispose();
                        return;
                    } else if (disposed) {
                        queue.clear();
                        return;
                    }
                    try {
//                        info("calling onMessage");
                        actor.onMessage(new MessageContext<T>(this, message.sender()), message.content());
//                        info("called onMessage");
                    } catch (Throwable e) {
                        // if the line below throws then the actor will no longer process messages
                        // (because wip will be != 0)
                        supervisor.processFailure(context, this, e);
                    }
                }
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
//        info("exited run, wip=" + wip.get());
    }

//    private void info(String s) {
//        log.info(name + ": " + s);
//    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public void restart() {
        actor = factory.get();
    }

    @Override
    public void clearQueue() {
        queue.clear();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public <S> CompletableFuture<S> ask(T message) {
        CompletableFuture<S> answer = new CompletableFuture<S>();
        ActorRef<S> actor = context.<S>matchAll((c, msg) -> answer.complete(msg)).build();
        tell(message, actor);
        return answer.whenComplete((result, error) -> {
            actor.dispose();
        });
    }

}
