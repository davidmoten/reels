package com.github.davidmoten.reels.internal.mailbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.Message;

public class MailboxUnboundedTest {

    @Test
    public void test() {
        Mailbox<Integer> m = new MailboxUnbounded<>();
        m.offer(new Message<>(1, ActorRef.none(), ActorRef.none()));
        m.offer(new Message<>(2, ActorRef.none(), ActorRef.none()));
        m.retryLatest();
        assertEquals(1, (int) m.poll().content());
        m.retryLatest();
        assertEquals(1, (int) m.poll().content());
        assertEquals(2, (int) m.poll().content());
        assertNull(m.poll());
    }

}
