package com.github.davidmoten.reels.internal.mailbox;

import java.util.ArrayDeque;

import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.Message;

public final class MailboxBounded<T> extends ArrayDeque<Message<T>> implements Mailbox<T> {

    private static final long serialVersionUID = 5507680744735201877L;

    private final int maxSize;

    private final boolean dropFirst;

    private Message<T> latest;
    private boolean retry;

    public MailboxBounded(int maxSize, boolean dropFirst) {
        this.maxSize = maxSize;
        this.dropFirst = dropFirst;
    }

    @Override
    public Message<T> poll() {
        if (retry && latest != null) {
            retry = false;
            return latest;
        } else {
            synchronized (this) {
                return latest = super.poll();
            }
        }
    }

    @Override
    public synchronized boolean offer(Message<T> message) {
        if (size() == maxSize) {
            if (dropFirst) {
                pollFirst();
            } else {
                return false;
            }
        }
        return super.offer(message);
    }

    @Override
    public void retryLatest() {
        retry = true;
    }

}
