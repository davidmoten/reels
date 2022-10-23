package com.github.davidmoten.reels.internal.mailbox;

import java.util.Comparator;
import java.util.PriorityQueue;

import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.Message;

public final class MailboxPriority<T> implements Mailbox<T> {

    private final PriorityQueue<Message<T>> queue;
    private Message<T> latest;
    private boolean retry;

    public MailboxPriority(Comparator<? super T> comparator) {
        queue = new PriorityQueue<Message<T>>((a, b) -> comparator.compare(a.content(), b.content()));
    }

    @Override
    public Message<T> poll() {
        if (retry && latest != null) {
            retry = false;
            return latest;
        } else {
            synchronized (this) {
                return latest = queue.poll();
            }
        }
    }

    @Override
    public synchronized boolean offer(Message<T> message) {
        return queue.offer(message);
    }

    @Override
    public void retryLatest() {
        retry = true;
    }

}
