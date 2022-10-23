package com.github.davidmoten.reels;

import java.util.function.Supplier;

import com.github.davidmoten.reels.internal.DeadLetterActor;
import com.github.davidmoten.reels.internal.Preconditions;

public final class ContextBuilder {

    private Supervisor supervisor = Supervisor.defaultSupervisor();
    private Supplier<? extends Actor<DeadLetter>> deadLetterActorFactory = () -> Context
            .createActorObject(DeadLetterActor.class);
    private Scheduler scheduler = Scheduler.defaultScheduler();
    private MailboxFactory mailboxFactory = MailboxFactory.unbounded();

    ContextBuilder() {
    }

    public ContextBuilder supervisor(Supervisor supervisor) {
        Preconditions.checkArgumentNonNull(supervisor, "supervisor");
        this.supervisor = supervisor;
        return this;
    }

    public ContextBuilder deadLetterActorFactory(Supplier<? extends Actor<DeadLetter>> factory) {
        Preconditions.checkArgumentNonNull(factory, "factory");
        this.deadLetterActorFactory = factory;
        return this;
    }
    
    public ContextBuilder scheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        return this;
    }
    
    public ContextBuilder mailboxFactory(MailboxFactory mailboxFactory) {
        this.mailboxFactory = mailboxFactory;
        return this;
    }

    public Context build() {
        return new Context(supervisor, deadLetterActorFactory, scheduler, mailboxFactory);
    }

}
