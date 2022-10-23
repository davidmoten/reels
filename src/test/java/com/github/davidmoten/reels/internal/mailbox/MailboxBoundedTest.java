package com.github.davidmoten.reels.internal.mailbox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Message;

public class MailboxBoundedTest {

    @Test
    public void testDropFirstAndRetry() {
         MailboxBounded<Integer> mailbox = new MailboxBounded<>(3, true);
         mailbox.offer(new Message<Integer>(1, ActorRef.none(), ActorRef.none()));
         mailbox.offer(new Message<Integer>(2, ActorRef.none(), ActorRef.none()));
         mailbox.offer(new Message<Integer>(3, ActorRef.none(), ActorRef.none()));
         mailbox.offer(new Message<Integer>(4, ActorRef.none(), ActorRef.none()));
         mailbox.offer(new Message<Integer>(5, ActorRef.none(), ActorRef.none()));
         mailbox.retryLatest();
         assertEquals(3, (int) mailbox.poll().content());
         mailbox.retryLatest();
         assertEquals(3, (int) mailbox.poll().content());
         assertEquals(4, (int) mailbox.poll().content());
         assertEquals(5, (int) mailbox.poll().content());
         assertNull(mailbox.poll());
    }
    
    @Test
    public void testDropLast() {
         MailboxBounded<Integer> mailbox = new MailboxBounded<>(3, false);
         mailbox.offer(new Message<Integer>(1, ActorRef.none(), ActorRef.none()));
         mailbox.offer(new Message<Integer>(2, ActorRef.none(), ActorRef.none()));
         mailbox.offer(new Message<Integer>(3, ActorRef.none(), ActorRef.none()));
         mailbox.offer(new Message<Integer>(4, ActorRef.none(), ActorRef.none()));
         mailbox.offer(new Message<Integer>(5, ActorRef.none(), ActorRef.none()));
         assertEquals(1, (int) mailbox.poll().content());
         assertEquals(2, (int) mailbox.poll().content());
         assertEquals(3, (int) mailbox.poll().content());
         assertNull(mailbox.poll());
    }


}
