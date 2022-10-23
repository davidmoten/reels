package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.Preconditions;
import com.github.davidmoten.reels.internal.mailbox.MailboxBoundedFactory;
import com.github.davidmoten.reels.internal.mailbox.MailboxUnboundedFactory;

@FunctionalInterface
public interface MailboxFactory {
    
    <T> Mailbox<T> create();
    
    static MailboxFactory unbounded() {
        return MailboxUnboundedFactory.INSTANCE;
    }
    
    static MailboxFactory bounded(int maxSize, boolean dropFirst) {
        Preconditions.checkArgument(maxSize > 0, "maxSize must be > 0");
        return new MailboxBoundedFactory(maxSize, dropFirst);
    }

}
