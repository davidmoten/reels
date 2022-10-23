package com.github.davidmoten.reels;

import com.github.davidmoten.reels.internal.mailbox.MailboxUnboundedFactory;

@FunctionalInterface
public interface MailboxFactory {
    
    <T> Mailbox<T> create();
    
    static MailboxFactory unbounded() {
        return MailboxUnboundedFactory.INSTANCE;
    }

}
