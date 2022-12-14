package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.supervisor.SupervisorDefault;

@FunctionalInterface
public interface Supervisor {

    /**
     * Processes an error thrown by {@link Actor#onMessage} or by
     * {@link Actor#onStop} or by {@link Actor#preStart}.
     * 
     * <p>
     * An error thrown by the {@link Actor#onStop} will be wrapped in an
     * {@link OnStopException}.
     * 
     * <p>
     * An error thrown by the {@link Actor#preStart} will be wrapped in a
     * {@link PreStartException}.
     * 
     * <P>
     * This method <b>must not throw</b>.
     * 
     * <p>
     * Note that because the method is provided with the actorRef, the dispose
     * method can be called on the actorRef which will stop all further processing
     * of messages including already queued ones. If the actorRef is not disposed
     * then message processing will continue with subsequent messages as normal.
     * 
     * <p>
     * A {@link SupervisedActorRef} can restart the actor (recreate the actor object
     * which discards current state in the actor) but retain the queued messages.
     * This should only be called synchronously in the processFailure method because
     * of type safety constraints.
     * 
     * @param message  message the actor failed to process (includes context
     *                 reference)
     * @param self reference to the actor where the error occurred
     * @param error    the error throw in the Actor.onMessage method. Wrapped in
     *                 OnStopException if thrown by onStop method, or
     *                 PreStartException if thrown by the preStart method.
     */
    void processFailure(Message<?> message, SupervisedActorRef<?> self, Throwable error);

    static Supervisor defaultSupervisor() {
        return SupervisorDefault.INSTANCE;
    }

}
