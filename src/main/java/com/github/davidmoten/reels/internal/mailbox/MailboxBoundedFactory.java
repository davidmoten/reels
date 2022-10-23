package com.github.davidmoten.reels.internal.mailbox;

import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.MailboxFactory;

public final class MailboxBoundedFactory implements MailboxFactory {

    private final int maxSize;
    private final boolean dropFirst;

    public MailboxBoundedFactory(int maxSize, boolean dropFirst) {
        this.maxSize = maxSize;
        this.dropFirst = dropFirst;
    }

    @Override
    public <T> Mailbox<T> create() {
        return new MailboxBounded<T>(maxSize, dropFirst);
    }

}
