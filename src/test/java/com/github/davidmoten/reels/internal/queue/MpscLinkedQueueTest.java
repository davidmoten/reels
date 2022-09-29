package com.github.davidmoten.reels.internal.queue;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MpscLinkedQueueTest {

    @Test(expected = NullPointerException.class)
    public void offerNullThrows() {
        MpscLinkedQueue<Object> q = new MpscLinkedQueue<>();
        q.offer(null);
    }
    
    @Test
    public void offerTwo() {
        MpscLinkedQueue<Object> q = new MpscLinkedQueue<>();
        q.offer(1, 2);
        assertEquals(1,  q.poll());
        assertEquals(2,  q.poll());
    }

}
