package com.github.davidmoten.reels.internal;

import static org.junit.Assert.*;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import com.github.davidmoten.reels.ActorRef;

public class ActorRefNoneTest {

    @Test
    public void test() {
        ActorRef<Object> a = ActorRef.none();
        a.tell(1);
        a.tell(1, a);
        assertEquals(null, a.child("child"));
        assertTrue(a.children().isEmpty());
        CompletableFuture<Object> f = a.ask(1);
        assertTrue(f.isCompletedExceptionally());
        assertTrue(a.isStopped());
        assertEquals(a, a.parent());
        assertEquals("reels-none", a.name());
        ActorRef.<Number>none().<Integer>narrow();
        ActorRef.<Integer>none().<Exception>recast();
        a.stop();
        a.stopNow();
        assertEquals(a.name(), a.toString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testContext() {
        ActorRef.none().context();
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testScheduler() {
        ActorRef.none().scheduler();
    }

    
}
