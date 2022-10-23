package com.github.davidmoten.reels.internal.mailbox;

import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.MailboxFactory;

public final class MailboxUnboundedFactory implements MailboxFactory {

    public static final MailboxUnboundedFactory INSTANCE = new MailboxUnboundedFactory();

    private MailboxUnboundedFactory() {
        // prevent instantiation
    }

    @Override
    public <T> Mailbox<T> create() {
        return new MailboxUnbounded<T>();
    }

}
