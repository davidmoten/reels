package com.github.davidmoten.reels.internal.mailbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Comparator;

import org.junit.Test;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Mailbox;
import com.github.davidmoten.reels.Message;

public class MailboxPriorityTest {

    @Test
    public void test() {
        Mailbox<Integer> m = new MailboxPriority<>(Comparator.naturalOrder());
        m.offer(new Message<Integer>(5, ActorRef.none(), ActorRef.none()));
        m.offer(new Message<Integer>(4, ActorRef.none(), ActorRef.none()));
        m.offer(new Message<Integer>(3, ActorRef.none(), ActorRef.none()));
        m.retryLatest();
        assertEquals(3, (int) m.poll().content());
        assertEquals(4, (int) m.poll().content());
        assertEquals(5, (int) m.poll().content());
        m.retryLatest();
        assertEquals(5, (int) m.poll().content());
        assertNull(m.poll());
    }

}
