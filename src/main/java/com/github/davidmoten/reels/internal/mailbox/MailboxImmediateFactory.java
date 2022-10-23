package com.github.davidmoten.reels.internal.mailbox;

import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.MailboxFactory;

public final class MailboxImmediateFactory implements MailboxFactory {
    
    public static final MailboxImmediateFactory INSTANCE = new MailboxImmediateFactory();

    @Override
    public <T> Mailbox<T> create() {
        return new MailboxImmediate<T>();
    }

}
