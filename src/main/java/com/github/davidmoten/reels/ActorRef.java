package com.github.davidmoten.reels;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.github.davidmoten.reels.internal.ActorRefNone;

/**
 * Reference that controls the lifecycle and operation of an Actor.
 * 
 * @param <T> the message type for the Actor
 */
public interface ActorRef<T> {

    /**
     * Sends the message without a sender.
     * 
     * @param message
     */
    void tell(T message);

    /**
     * Sends the message with the given sender.
     * 
     * @param message message to send
     * @param sender  message sender (for replies as an example)
     */
    void tell(T message, ActorRef<?> sender);

    /**
     * Creates a temporary actor that sends the message to {@code this} and the
     * returned {@link CompletableFuture} waits on a response. The arrival of the
     * response to the Future or a timeout on CompletableFuture.get also disposes
     * the temporary actor as does cancelling the Future.
     * 
     * @param <S>     type of response
     * @param message message to send to {@code this}
     * @return future
     */
    <S> CompletableFuture<S> ask(T message);

    /**
     * Sends a Poison Pill message to the actor which will be stopped when that
     * message is processed. The Poison Pill message does not jump the queue past
     * other already waiting messages on the Actor.
     */
    void stop();

    /**
     * Sends a Poison Pill message to the actor which will be stopped when that
     * message is processed. All messages before it in the queue are removed from
     * the queue and sent to the Dead Letter actor so the Poison Pill message
     * effectively jumps the queue.
     */
    void stopNow();

    boolean isStopped();

    /**
     * Returns the current actor system context.
     * 
     * @return actor system context
     */
    Context context();

    /**
     * The unique Actor identifier for the current Context.
     * 
     * @return unique identifier for the current Context
     */
    String name();

    /**
     * Returns the scheduler that the Actor uses to process messages.
     * 
     * @return the scheduler that the Actor uses to process messages
     */
    Scheduler scheduler();

    /**
     * Returns the parent of this actor.
     * 
     * @return parent actor reference
     */
    <S> ActorRef<S> parent();

    /**
     * Returns the child of this actor with the given name. Returns null if not
     * found.
     * 
     * @param <S>  message type of child actor
     * @param name name of actor
     * @return the child of this actor with the given name. Returns null if not
     *         found
     */
    <S> ActorRef<S> child(String name);

    /**
     * Returns the children of this.
     * 
     * @param <S> children type (use Object) if unknown
     * @return children of this
     */
    <S> Collection<ActorRef<S>> children();

    /**
     * Returns type-safe recasting of ActorRef message type.
     * 
     * @param <S> new message type
     * @return this but with different generic typing
     */
    @SuppressWarnings("unchecked")
    default <S> ActorRef<S> recast() {
        return (ActorRef<S>) this;
    }

    /**
     * Returns type-safe recasting of ActorRef message type.
     * 
     * @param <S> new message type
     * @return this but with different generic typing
     */
    @SuppressWarnings("unchecked")
    default <S extends T> ActorRef<S> narrow() {
        return (ActorRef<S>) this;
    }

    /**
     * Returns a singleton ActorRef that does nothing (with whatever generic type
     * for your convenience).
     * 
     * @param <S> message type of ActorRef
     * @return do nothing ActorRef
     */
    @SuppressWarnings("unchecked")
    static <S> ActorRef<S> none() {
        return (ActorRef<S>) ActorRefNone.NONE;
    }

}
