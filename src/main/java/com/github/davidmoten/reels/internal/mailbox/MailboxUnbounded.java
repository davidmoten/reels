package com.github.davidmoten.reels.internal.mailbox;

import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.internal.queue.MpscLinkedQueue;

public final class MailboxUnbounded<T> extends MpscLinkedQueue<Message<T>> implements Mailbox<T> {
    
    private Message<T> latest;
    private boolean retry;

    @Override
    public Message<T> poll() {
        if (retry && latest != null) {
            retry = false;
            return latest;
        } else {
            return latest = super.poll();
        }
    }

    @Override
    public void retryLatest() {
        retry = true;
    }

}
