package com.github.davidmoten.reels.internal.mailbox;

import java.util.LinkedList;
import java.util.Queue;

import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.Message;

/**
 * A Mailbox without synchronization. Not thread-safe.
 * 
 * @param <T>
 */
public final class MailboxUnsynchronized<T> implements Mailbox<T> {

    private final Queue<Message<T>> queue = new LinkedList<Message<T>>();

    private Message<T> latest;
    private boolean retry;

    @Override
    public Message<T> poll() {
        if (retry && latest != null) {
            retry = false;
            return latest;
        } else {
            return latest = queue.poll();
        }
    }

    @Override
    public void retryLatest() {
        retry = true;
    }

    @Override
    public boolean offer(Message<T> message) {
        return queue.offer(message);
    }

}
