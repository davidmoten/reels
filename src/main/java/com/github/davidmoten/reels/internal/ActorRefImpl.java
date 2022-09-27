package com.github.davidmoten.reels.internal;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.OnStopException;
import com.github.davidmoten.reels.PoisonPill;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.SupervisedActorRef;
import com.github.davidmoten.reels.Supervisor;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.queue.MpscLinkedQueue;
import com.github.davidmoten.reels.internal.queue.SimplePlainQueue;
import com.github.davidmoten.reels.internal.util.OpenHashSet;

public final class ActorRefImpl<T> extends AtomicInteger implements SupervisedActorRef<T>, Runnable, Disposable {

    private static final Logger log = LoggerFactory.getLogger(ActorRefImpl.class);

    private static final long serialVersionUID = 8766398270492289693L;
    private final String name;
    private final Supplier<? extends Actor<T>> factory;
    private transient final SimplePlainQueue<Message<T>> queue; // mailbox
    private final Context context;
    private final Supervisor supervisor;
    private final Scheduler scheduler;
    private final Worker worker;
    private final ActorRef<?> parent; // nullable
    private OpenHashSet<ActorRef<?>> children; // synchronized, nullable, lazily assigned
    private Actor<T> actor; // mutable because recreated if restart called
    private volatile int state = ACTIVE; // 0 = ACTIVE, 1 = STOPPING, 2 = STOPPED, 3 = DISPOSED

    private static final int ACTIVE = 0;
    private static final int STOPPING = 1;
    private static final int STOPPED = 2;
    private static final int DISPOSED = 3;

    public static <T> ActorRefImpl<T> create(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler,
            Context context, Supervisor supervisor, ActorRef<?> parent) {
        ActorRefImpl<T> a = new ActorRefImpl<T>(name, factory, scheduler, context, supervisor, parent);
        if (parent != null) {
            ((ActorRefImpl<?>) parent).addChild(a);
        }
        return a;
    }

    private ActorRefImpl(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler, Context context,
            Supervisor supervisor, ActorRef<?> parent) {
        super();
        this.name = name;
        this.factory = factory;
        this.context = context;
        this.supervisor = supervisor;
        this.queue = new MpscLinkedQueue<Message<T>>();
        this.worker = scheduler.createWorker();
        this.parent = parent;
        this.actor = factory.get();
        this.scheduler = scheduler;
    }

    void addChild(ActorRef<?> actor) {
        synchronized (name) {
            if (state == DISPOSED) {
                actor.dispose();
            } else {
                children().add(actor);
            }
        }
    }

    void removeChild(ActorRef<?> actor) {
        synchronized (name) {
            children().remove(actor);
        }
    }

    @Override
    public void dispose() {
        synchronized (name) {
            disposeThis();
            disposeChildren();
        }
    }

    private void disposeChildren() {
        if (children != null) {
            for (Object child : children.keys()) {
                if (child != null) {
                    ((ActorRef<?>) child).dispose();
                }
            }
        }
    }

    public void disposeThis() {
        if (state != DISPOSED) {
            state = DISPOSED;
            worker.dispose();
            queue.clear();
            if (parent != null) {
                ((ActorRefImpl<?>) parent).removeChild(this);
            }
            context.disposed(this);
        }
    }

    @Override
    public void tell(T message) {
        tell(message, null);
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        info("sending " + message + " to " + sender);
        if (state == DISPOSED) {
            return;
        }
        queue.offer(new Message<T>(message, this, sender));
        worker.schedule(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void stop() {
        tell((T) PoisonPill.INSTANCE);
    }

    private OpenHashSet<ActorRef<?>> children() {
        if (children == null) {
            children = new OpenHashSet<>();
        }
        return children;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        // drain queue
//        info("run called");
        if (getAndIncrement() == 0) {
//            info("starting drain");
            while (true) {
                int missed = 1;
                Message<T> message;
                while ((message = queue.poll()) != null) {
                    info("message polled=" + message.content());
                    int s = state;
                    if (s == DISPOSED) {
                        queue.clear();
                        return;
                    } else if (s == STOPPING) {
                        if (message.content() == Constants.TERMINATED) {
                            children.remove(message.senderRaw());
                            if (children.isEmpty()) {
                                s = STOPPED;
                                try {
                                    actor.onStop(message);
                                } catch (Throwable e) {
                                    supervisor.processFailure(message, this, new OnStopException(e));
                                    // TODO catch throw
                                }
                                if (message.senderRaw() != null) {
                                    message.senderRaw().tell(Constants.TERMINATED, message.self().parent());
                                }
                                context.actorStopped(this);
                            }
                        } else {
                            context.deadLetterActor().tell(message, this);
                        }
                    } else if (s == STOPPED) {
                        context.deadLetterActor().tell(message, this);
                    } else if (message.content() == PoisonPill.INSTANCE) {
                        state = STOPPING;
                        OpenHashSet<ActorRef<?>> copy = null;
                        synchronized (name) {
                            if (children != null) {
                                copy = new OpenHashSet<>();
                                for (Object child : children().keys()) {
                                    if (child != null) {
                                        copy.add((ActorRef<?>) child);
                                    }
                                }
                            }
                        }
                        if (copy != null) {
                            // we send stop message outside of synchronized block
                            // because immediate scheduler might be in use
                            for (Object child : copy.keys()) {
                                if (child != null) {
                                    ((ActorRef<Object>) child).tell(PoisonPill.INSTANCE, this);
                                }
                            }
                        } else {
                            if (message.senderRaw() != null) {
                                message.senderRaw().tell(Constants.TERMINATED, message.senderRaw());
                            }
                        }
                        return;
                    } else {
                        try {
//                        info("calling onMessage");
                            actor.onMessage(message);
//                        info("called onMessage");
                        } catch (Throwable e) {
                            // if the line below throws then the actor will no longer process messages
                            // (because wip will be != 0)
                            supervisor.processFailure(message, this, e);
                            // TODO catch throw
                        }
                    }
                }
                missed = addAndGet(-missed);
                if (missed == 0) {
                    break;
                }
            }
        }
//        info("exited run, wip=" + wip.get());
    }

    private void info(String s) {
        log.debug("{}: {}", name, s);
    }

    @Override
    public boolean isDisposed() {
        return state == DISPOSED;
    }

    public boolean isStopped() {
        return state == STOPPED;
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
        AskFuture<S> future = new AskFuture<S>();
        ActorRef<S> actor = context.<S>matchAll(m -> future.setValue(m.content())).build();
        future.setDisposable(actor);
        tell(message, actor);
        return future;
    }

    @Override
    public String toString() {
        return name;
    }

    // VisibleForTesting
    static final class AskFuture<T> extends CompletableFuture<T> {

        private final AtomicBoolean disposed;
        private Disposable disposable = Disposable.disposed(); // mutable

        public AskFuture() {
            super();
            this.disposed = new AtomicBoolean();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            super.cancel(mayInterruptIfRunning);
            dispose();
            return true;
        }

        @Override
        public boolean isCancelled() {
            return disposable.isDisposed();
        }

        private void dispose() {
            if (disposed.compareAndSet(false, true)) {
                disposable.dispose();
            }
        }

        public void setDisposable(Disposable disposable) {
            this.disposable = disposable;
        }

        public void setValue(T value) {
            complete(value);
            dispose();
        }
    }

    @Override
    public ActorRef<?> parent() {
        return parent;
    }

}
