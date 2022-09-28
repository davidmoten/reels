package com.github.davidmoten.reels.internal;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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

public class ActorRefImpl<T> extends AtomicInteger implements SupervisedActorRef<T>, Runnable, Disposable {

    private static final boolean debug = false;
    private static final Logger log = LoggerFactory.getLogger(ActorRefImpl.class);

    private static final long serialVersionUID = 8766398270492289693L;
    private final String name;
    private final Supplier<? extends Actor<T>> factory; // used to recreate actor
    private transient final SimplePlainQueue<Message<T>> queue; // mailbox
    private final Context context;
    private final Supervisor supervisor;
    private final Scheduler scheduler;
    private final Worker worker;
    private final ActorRef<?> parent; // nullable
    private final Map<String, ActorRef<?>> children; // concurrent
    private Actor<T> actor; // mutable because recreated if restart called
    protected volatile int state = ACTIVE;
    protected static final int ACTIVE = 0;
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

    protected ActorRefImpl(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler, Context context,
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
        this.children = new ConcurrentHashMap<>();
    }

    private void addChild(ActorRef<?> actor) {
        if (state == DISPOSED) {
            actor.dispose();
        } else {
            children.put(actor.name(), actor);
        }
    }

    void removeChild(ActorRef<?> actor) {
        children.remove(actor.name());
    }

    @Override
    public void dispose() {
        if (debug)
            log("disposing");
        // use a stack rather than recursion to avoid
        // stack overflow on deeply nested heirarchies
        Deque<ActorRef<?>> stack = new LinkedList<>();
        stack.offer(this);
        ActorRef<?> a;
        while ((a = stack.poll()) != null) {
            ((ActorRefImpl<?>) a).disposeThis();
            stack.addAll(children.values());
        }
        if (debug)
            log("disposed");
    }

    public void disposeThis() {
        if (state != DISPOSED) {
            state = DISPOSED;
            worker.dispose();
            queue.clear();
            if (parent != null) {
                ((ActorRefImpl<?>) parent).removeChild(this);
            }
        }
    }

    @Override
    public void tell(T message) {
        tell(message, null);
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        if (state == DISPOSED) {
            return;
        }
//        info(message + " arrived from " + sender + " to " + this);
        queue.offer(new Message<T>(message, this, sender));
        worker.schedule(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void stop() {
        tell((T) PoisonPill.INSTANCE, parent);
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
                    int s = state;
                    if (debug)
                        log("message polled=" + message.content() + ", state=" + s);
                    if (s == DISPOSED) {
                        queue.clear();
                        return;
                    } else if (s == STOPPING) {
                        if (message.content() == Constants.TERMINATED) {
                            handleTerminationMessage(message);
                        } else {
                            sendToDeadLetter(message);
                        }
                    } else if (s == STOPPED) {
                        if (message.content() != PoisonPill.INSTANCE) {
                            sendToDeadLetter(message);
                        }
                    } else if (message.content() == PoisonPill.INSTANCE) {
                        state = STOPPING;
                        boolean isEmpty = true;
                        for (ActorRef<?> child : children.values()) {
                            ((ActorRef<Object>) child).tell(PoisonPill.INSTANCE, this);
                            isEmpty = false;
                        }
                        if (isEmpty) {
                            // no children, run onStop and send Terminated to parent
                            runOnStop(message);
                        }
                    } else if (message.content() == Constants.TERMINATED) {
                        handleTerminationMessage(message);
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
    }

    private void handleTerminationMessage(Message<T> message) {
        children.remove(message.senderRaw().name());
        if (children.isEmpty()) {
            runOnStop(message);
        }
    }

    private void sendToDeadLetter(Message<T> message) {
        if (context.deadLetterActor() != this) {
            context.deadLetterActor().tell(message, this);
        }
    }

    @SuppressWarnings("unchecked")
    private void runOnStop(Message<T> message) {
        state = STOPPED;
        try {
            actor.onStop(message);
        } catch (Throwable e) {
            supervisor.processFailure(message, this, new OnStopException(e));
            // TODO catch throw
        }
        complete();
        ActorRef<?> p = parent;
        if (p == null) {
            // must be root actor
            p = message.self();
            queue.offer(new Message<T>((T) Constants.TERMINATED, this, this));
        } else {
            p.<Object>recast().tell(Constants.TERMINATED, this);
        }
    }

    private void log(String s) {
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

    @Override
    public ActorRef<?> child(String name) {
        return children.get(name);
    }
    
    protected void complete() {
        // do nothing
    }

}
