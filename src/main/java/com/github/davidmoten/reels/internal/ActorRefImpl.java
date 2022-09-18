package com.github.davidmoten.reels.internal;

import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
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

    public static final Object POISON_PILL = new Object();

    private final String name;
    private final Supplier<? extends Actor<T>> factory;
    private transient final SimplePlainQueue<Message<T>> queue; // mailbox
    private final Context context;
    private final Supervisor supervisor;
    private final Scheduler scheduler;
    private final Worker worker;
    private final Optional<ActorRef<?>> parent;
    private final ConcurrentHashMap<ActorRef<?>, ActorRef<?>> children;
    private final AtomicInteger wip;
    private Actor<T> actor; // mutable because recreated if restart called
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
        this.wip = new AtomicInteger();
    }

    void addChild(ActorRef<?> actor) {
        children.put(actor, actor);
    }

    void removeChild(ActorRef<?> actor) {
        children.remove(actor);
    }

    @Override
    public void dispose() {
        disposeThis();
        disposeChildren();
    }

    private void disposeChildren() {
        Enumeration<ActorRef<?>> en = children.keys();
        while (en.hasMoreElements()) {
            ActorRef<?> child = en.nextElement();
            child.dispose();
        }
    }

    private void disposeThis() {
        if (!disposed) {
            disposed = true;
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
                        stopChildren();
                        disposeThis();
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

    private void stopChildren() {
        Enumeration<ActorRef<?>> en = children.keys();
        while (en.hasMoreElements()) {
            ActorRef<?> child = en.nextElement();
            child.stop();
        }
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
    public <S> Future<S> ask(T message) {
        AskFuture<S> future = new AskFuture<S>();
        ActorRef<S> actor = context.<S>matchAll((c, msg) -> future.setValue(msg)).build();
        future.setDisposable(actor);
        tell(message, actor);
        return future;
    }

    private static final class AskFuture<T> extends CountDownLatch implements Future<T> {

        private final AtomicBoolean disposed;
        private Disposable disposable = Disposable.disposed();
        private volatile T value;

        public AskFuture() {
            super(1);
            this.disposed = new AtomicBoolean();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            countDown();
            dispose();
            return true;
        }

        @Override
        public boolean isCancelled() {
            return disposable.isDisposed();
        }

        @Override
        public boolean isDone() {
            return value != null;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            await();
            return value;
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (await(timeout, unit)) {
                return value;
            } else {
                dispose();
                throw new TimeoutException();
            }
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
            this.value = value;
            countDown();
            dispose();
        }

    }

}
