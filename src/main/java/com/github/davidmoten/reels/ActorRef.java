package com.github.davidmoten.reels;

public interface ActorRef<T> extends Disposable {

    /**
     * Sends the message without a sender.
     * 
     * @param message
     */
    public void tell(T message);

    public void tell(T message, ActorRef<?> sender);

    public void dispose();

}
