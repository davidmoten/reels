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
import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.MailboxFactory;
import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.OnStopException;
import com.github.davidmoten.reels.PoisonPill;
import com.github.davidmoten.reels.PreStartException;
import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.SupervisedActorRef;
import com.github.davidmoten.reels.Supervisor;
import com.github.davidmoten.reels.Worker;

public abstract class ActorRefImpl<T> implements SupervisedActorRef<T>, Runnable {

    public static final boolean debug = false;
    private static final Logger log = LoggerFactory.getLogger(ActorRefImpl.class);

    private final String name;
    private final Supplier<? extends Actor<T>> factory; // used to recreate actor
    private transient final Mailbox<T> mailbox; // mailbox
    private final Context context;
    private final Supervisor supervisor;
    private final Scheduler scheduler;
    private final Worker worker;
    private final ActorRef<?> parent; // nullable
    private final Map<String, ActorRef<?>> children; // concurrent
    private Actor<T> actor; // mutable because recreated if restart called
    private boolean preStartHasBeenRun;
    protected final AtomicInteger state = new AtomicInteger(); // ACTIVE
    private boolean systemMessagesOnly;

    private static final int ACTIVE = 0;
    private static final int STOPPING = 1;
    private static final int STOPPED = 2;
    private static final int STOPPING_NOW = 3;
    protected static final int RESTART = 4;
    private static final int PAUSED = 5;

    public static <T> ActorRefImpl<T> create(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler,
            Context context, Supervisor supervisor, ActorRef<?> parent, MailboxFactory mailboxFactory) {
        final ActorRefImpl<T> a;
        if (scheduler.requiresDrainSynchronization()) {
            a = new ActorRefSynchronizedDrain<T>(name, factory, scheduler, context, supervisor, parent, mailboxFactory);
        } else {
            a = new ActorRefUnsynchronizedDrain<T>(name, factory, scheduler, context, supervisor, parent, mailboxFactory);
        }
        if (parent != null) {
            ((ActorRefImpl<?>) parent).addChild(a);
        }
        return a;
    }

    protected ActorRefImpl(String name, Supplier<? extends Actor<T>> factory, Scheduler scheduler, Context context,
            Supervisor supervisor, ActorRef<?> parent, MailboxFactory mailboxFactory) {
        this.name = name;
        this.factory = factory;
        this.context = context;
        this.supervisor = supervisor;
        this.mailbox = mailboxFactory.create();
        this.worker = scheduler.createWorker();
        this.parent = parent;
        this.scheduler = scheduler;
        this.children = new ConcurrentHashMap<>();
        createActor();
    }

    private void addChild(ActorRef<?> actor) {
        int s = state.get();
        if (s == STOPPING_NOW || s == STOPPING || s == STOPPED) {
            actor.stopNow();
        } else {
            children.put(actor.name(), actor);
        }
    }

    @Override
    public void stop() {
        tell(PoisonPill.instance(), parent);
    }

    @Override
    public void stopNow() {
        if (debug)
            log("stopNow");
        // use a stack rather than recursion to avoid
        // stack overflow on deeply nested hierarchies
        Deque<ActorRef<?>> stack = new ArrayDeque<>();
        stack.offer(this);
        ActorRef<?> a;
        while ((a = stack.poll()) != null) {
            ((ActorRefImpl<?>) a).stopNowThis();
            stack.addAll(children.values());
        }
    }

    public void stopNowThis() {

        while (true) {
            final int s = state.get();
            if (s == STOPPING_NOW || s == STOPPING || s == STOPPED) {
                break;
            } else if (state.compareAndSet(s, STOPPING_NOW)) {
                if (debug) {
                    log("removing from parent and calling stop");
                }
                stop();
                break;
            }
        }
    }

    private boolean setState(int value) {
        while (true) {
            int s = state.get();
            if (s == STOPPED || s == STOPPING && value == STOPPING_NOW) {
                return false;
            }
            if (state.compareAndSet(s, value)) {
                return true;
            }
        }
    }

    @Override
    public boolean isStopped() {
        return state.get() == STOPPED;
    }

    @Override
    public void tell(T message) {
        tell(message, ActorRef.none());
    }

    @Override
    public void tell(T message, ActorRef<?> sender) {
        mailbox.offer(new Message<T>(message, this, sender));
        scheduleDrain();
    }

    private void scheduleDrain() {
        worker.schedule(this);
    }

    private void handleTerminationMessage(Message<T> message) {
        children.remove(message.sender().name());
        if (children.isEmpty()) {
            runOnStop(message);
        }
    }

    private void sendToDeadLetter(Message<T> message) {
        if (context.deadLetterActor() != this && !systemMessagesOnly) {
            context.deadLetterActor().tell(new DeadLetter(message), this);
        }
    }

    @SuppressWarnings("unchecked")
    private void runOnStop(Message<T> message) {
        if (setState(STOPPED)) {
            if (debug) {
                log(this + ": running onStop with message=" + message);
            }
            try {
                actor.onStop(this);
            } catch (Throwable e) {
                supervisor.processFailure(message, this, new OnStopException(e));
            }
            complete();
            ActorRef<?> p = parent;
            if (p == null) {
                // is root actor (which is the only actor without a parent)
                mailbox.offer(new Message<T>((T) Terminated.INSTANCE, this, this));
            } else {
                p.<Object>recast().tell(Terminated.INSTANCE, this);
            }
        }
    }

    private void log(String s) {
        log.debug("{}: {}", name, s);
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
    protected final void finalize() throws Throwable {
        // do nothing, stops spotbugs from complaining about 
        // exception leaving the constructor
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
        mailbox.retryLatest();
    }

    @SuppressWarnings("unchecked")
    protected void drain() {
        Message<T> message;
        int s;
        while ((s = state.get()) != PAUSED && (message = (Message<T>) mailbox.poll()) != null) {
            if (debug) {
                log("message polled=" + message.content() + " from " + message.sender() + ", state=" + s);
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
            if (s == STOPPING_NOW) {
                systemMessagesOnly = true;
            }
            if (s == STOPPING) {
                if (message.content() == Terminated.INSTANCE) {
                    handleTerminationMessage(message);
                } else {
                    sendToDeadLetter(message);
                }
            } else if (s == STOPPED) {
                if (message.content() != PoisonPill.instance()) {
                    sendToDeadLetter(message);
                }
            } else if (message.content() == PoisonPill.instance()) {
                if (setState(STOPPING)) {
                    boolean isEmpty = true;
                    ActorRef<Object> ch;
                    for (ActorRef<?> child : children.values()) {
                        ch = (ActorRef<Object>) child;
                        if (systemMessagesOnly) {
                            ch.stopNow();
                        } else {
                            ch.stop();
                        }
                        isEmpty = false;
                    }
                    if (isEmpty) {
                        // no children, run onStop and send Terminated to parent
                        runOnStop(message);
                    }
                }
            } else if (message.content() == Terminated.INSTANCE) {
                handleTerminationMessage(message);
            } else if (systemMessagesOnly) {
                sendToDeadLetter(message);
            } else {
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
