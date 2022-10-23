package com.github.davidmoten.reels;

public interface Mailbox<T> {

    /**
     * If mailbox empty returns null otherwise returns first message available on
     * the the queue and removes it. Should only be called from one thread (that is
     * serially).
     * 
     * @return first message available or null if mailbox empty
     */
    Message<T> poll();

    /**
     * Adds the message to the mailbox. In general this method is thread-safe (can
     * be called concurrently).
     * 
     * @param value message to add to the mailbox
     * @return true if added to the mailbox
     */
    boolean offer(Message<T> message);

    /**
     * Places the last message polled back on the queue so that the next poll will
     * return that message again.
     */
    void retryLatest();
}
