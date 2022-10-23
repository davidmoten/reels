package com.github.davidmoten.reels.internal.mailbox;

import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.MailboxFactory;

public final class MailboxUnsynchronizedFactory implements MailboxFactory {
    
    public static final MailboxUnsynchronizedFactory INSTANCE = new MailboxUnsynchronizedFactory();

    @Override
    public <T> Mailbox<T> create() {
        return new MailboxUnsynchronized<T>();
    }

}
