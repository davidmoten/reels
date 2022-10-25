package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.github.davidmoten.reels.ActorRef;

public class ActorRefCompletableFutureTest {

    @Test
    public void testTell() throws InterruptedException, ExecutionException {
        ActorRefCompletableFuture<Integer> f = new ActorRefCompletableFuture<Integer>();
        f.tell(1);
        assertEquals(1, (int) f.get());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAsk() {
        ActorRefCompletableFuture<Integer> f = new ActorRefCompletableFuture<Integer>();
        f.ask(1);
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testContext() {
        ActorRefCompletableFuture<Integer> f = new ActorRefCompletableFuture<Integer>();
        f.context();
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testScheduler() {
        ActorRefCompletableFuture<Integer> f = new ActorRefCompletableFuture<Integer>();
        f.scheduler();
    }
    
    @Test
    public void testVarious() {
        ActorRefCompletableFuture<Integer> f = new ActorRefCompletableFuture<Integer>();
        f.stop();
        f.stopNow();
        assertEquals("reels-actor-ref-completable-future", f.name());
        assertEquals(ActorRef.none(), f.parent());
        assertNull(f.child("child"));
        assertTrue(f.children().isEmpty());
        assertFalse(f.isStopped());
    }
    
}
