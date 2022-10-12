package com.github.davidmoten.reels.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.Actor;
import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.CreateException;
import com.github.davidmoten.reels.DeadLetter;
import com.github.davidmoten.reels.Disposable;
import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.OnStopException;
import com.github.davidmoten.reels.PoisonPill;
import com.github.davidmoten.reels.PreStartException;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.SupervisedActorRef;
import com.github.davidmoten.reels.Supervisor;
import com.github.davidmoten.reels.Worker;
import com.github.davidmoten.reels.internal.queue.MpscLinkedQueue;
import com.github.davidmoten.reels.internal.queue.SimplePlainQueue;

public abstract class ActorRefImpl<T> implements SupervisedActorRef<T>, Runnable, Disposable {

    public static final boolean debug = false;
    private static final Logger log = LoggerFactory.getLogger(ActorRefImpl.class);

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
    private boolean preStartHasBeenRun;
    protected final AtomicInteger state = new AtomicInteger(); // ACTIVE
    private Message<T> lastMessage; // used for retrying
    private boolean retry;
    private boolean ignoreNonSystemMessages;

    // TODO use enum
    private static final int ACTIVE = 0;
    private static final int STOPPING = 1;
    private static final int STOPPED = 2;
    private static final int DISPOSING = 3;
    protected static final int RESTART = 4;
    private static final int PAUSED = 5;

    public static <T> ActorRefImpl<T> create(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler,
            Context context, Supervisor supervisor, ActorRef<?> parent) {
        final ActorRefImpl<T> a;
        if (scheduler.requiresSerialization()) {
            a = new ActorRefSerialized<T>(name, factory, scheduler, context, supervisor, parent);
        } else {
            a = new ActorRefNotSerialized<T>(name, factory, scheduler, context, supervisor, parent);
        }
        if (parent != null) {
            ((ActorRefImpl<?>) parent).addChild(a);
        }
        return a;
    }

    protected ActorRefImpl(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler, Context context,
            Supervisor supervisor, ActorRef<?> parent) {
        this.name = name;
        this.factory = factory;
        this.context = context;
        this.supervisor = supervisor;
        this.queue = new MpscLinkedQueue<Message<T>>();
        this.worker = scheduler.createWorker();
        this.parent = parent;
        this.scheduler = scheduler;
        this.children = new ConcurrentHashMap<>();
        createActor();
    }

    private void addChild(ActorRef<?> actor) {
        int s = state.get();
        // TODO use enum method here
        if (s == DISPOSING || s == STOPPING || s == STOPPED) {
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
        // stack overflow on deeply nested hierarchies
        Deque<ActorRef<?>> stack = new ArrayDeque<>();
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
        if (debug) {
            log("disposed");
        }
        while (true) {
            final int s = state.get();
            // TODO use enum
            if (s == DISPOSING || s == STOPPING || s == STOPPED) {
                break;
            } else if (state.compareAndSet(s, DISPOSING)) {
                if (parent != null) {
                    ((ActorRefImpl<?>) parent).removeChild(this);
                }
                stop();
                break;
            }
        }
    }

    private boolean setState(int value) {
        while (true) {
            int s = state.get();
            // TODO disallow bad transitions
            if (state.compareAndSet(s, value)) {
                return true;
            }
        }
    }

    @Override
    public void tell(T message) {
        tell(message, ActorRef.none());
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        if (state.get() == DISPOSING) {
            return;
        }
        queue.offer(new Message<T>(message, this, sender));
        scheduleDrain();
    }

    private void scheduleDrain() {
        worker.schedule(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void stop() {
        tell((T) PoisonPill.INSTANCE, parent);
    }

    private void handleTerminationMessage(Message<T> message) {
        children.remove(message.sender().name());
        if (children.isEmpty()) {
            runOnStop(message);
        }
    }

    private void sendToDeadLetter(Message<T> message) {
        if (context.deadLetterActor() != this && !ignoreNonSystemMessages) {
            context.deadLetterActor().tell(new DeadLetter(message), this);
        }
    }

    @SuppressWarnings("unchecked")
    private void runOnStop(Message<T> message) {
        if (setState(STOPPED)) {
            try {
                actor.onStop(this);
            } catch (Throwable e) {
                supervisor.processFailure(message, this, new OnStopException(e));
            }
            complete();
            ActorRef<?> p = parent;
            if (p == null) {
                // is root actor (which is the only actor without a parent)
                queue.offer(new Message<T>((T) Constants.TERMINATED, this, this));
            } else {
                p.<Object>recast().tell(Constants.TERMINATED, this);
            }
        }
    }

    private void log(String s) {
        log.debug("{}: {}", name, s);
    }

    @Override
    public boolean isDisposed() {
        return state.get() == DISPOSING;
    }

    @Override
    public Context context() {
        return context;
    }

    @Override
    public boolean restart() {
        while (true) {
            int s = state.get();
            if (s == ACTIVE || s == PAUSED) {
                if (state.compareAndSet(s, RESTART)) {
                    run();
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean pause(long duration, TimeUnit unit) {
        if (state.compareAndSet(ACTIVE, PAUSED)) {
            scheduler.schedule(() -> state.compareAndSet(PAUSED, ACTIVE), duration, unit);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean pauseAndRestart(long delay, TimeUnit unit) {
        if (pause(delay, unit)) {
            scheduler.schedule(this::restart, delay, unit);
            return true;
        } else {
            return false;
        }
    }

    private Actor<T> createActor() {
        actor = factory.get();
        if (actor == null) {
            throw new CreateException("actor factory cannot return null");
        }
        preStartHasBeenRun = false;
        return actor;
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
        ActorRefCompletableFuture<S> actor = new ActorRefCompletableFuture<>();
        tell(message, actor);
        return actor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ActorRef<S> parent() {
        return (ActorRef<S>) parent;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> ActorRef<S> child(String name) {
        return (ActorRef<S>) children.get(name);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> Collection<ActorRef<S>> children() {
        return (Collection<ActorRef<S>>) (Collection<?>) new ArrayList<>(children.values());
    }

    protected void complete() {
        // do nothing
    }

    public Supervisor supervisor() {
        return supervisor;
    }

    @Override
    public void retry() {
        retry = true;
    }

    private Message<T> poll() {
        if (retry) {
            retry = false;
            return lastMessage;
        }
        Message<T> v = queue.poll();
        lastMessage = v;
        return v;
    }

    @SuppressWarnings("unchecked")
    protected void drain() {
        Message<T> message;
        int s;
        while ((s = state.get()) != PAUSED && (message = poll()) != null) {
            if (debug) {
                log("message polled=" + message.content() + ", state=" + s);
            }
            if (s == RESTART) {
                actor.onStop(this);
                createActor();
                if (setState(ACTIVE)) {
                    s = ACTIVE;
                } else {
                    s = state.get();
                }
            }
            if (s == DISPOSING) {
                ignoreNonSystemMessages = true;
            }
            if (s == STOPPING) {
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
                if (setState(STOPPING)) {
                    boolean isEmpty = true;
                    for (ActorRef<?> child : children.values()) {
                        ((ActorRef<Object>) child).tell(PoisonPill.INSTANCE, this);
                        isEmpty = false;
                    }
                    if (isEmpty) {
                        // no children, run onStop and send Terminated to parent
                        runOnStop(message);
                    }
                }
            } else if (message.content() == Constants.TERMINATED) {
                handleTerminationMessage(message);
            } else if (!ignoreNonSystemMessages) {
                if (!preStartHasBeenRun) {
                    runPreStart(message);
                }
                try {
                    actor.onMessage(message);
                } catch (Throwable e) {
                    // if the line below throws then the actor will no longer process messages
                    // (because wip will be != 0)
                    supervisor.processFailure(message, this, e);
                }
            }
        }

    }

    private void runPreStart(Message<T> message) {
        try {
            actor.preStart(this);
            preStartHasBeenRun = true;
        } catch (Throwable e) {
            // if the line below throws then the actor will no longer process messages
            // (because wip will be != 0)
            supervisor.processFailure(message, this, new PreStartException(e));
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
