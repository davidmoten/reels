package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.davidmoten.reels.Context;
import com.github.davidmoten.reels.Scheduler;

public class ActorRefDisposedTest {

    @Test
    public void testActorRefDisposed() {
        Context context = Context.create();
        ActorRefDisposed<Boolean> a = new ActorRefDisposed<>(context, "a");
        assertEquals(context, a.context());
        assertTrue(a.isDisposed());
        a.stop();
        a.dispose();
        assertTrue(a.isDisposed());
        assertEquals("a", a.name());
        a.tell(false);
        a.tell(false, a);
        assertTrue(a.ask(false) instanceof CancelledCompletableFuture);
        assertTrue(Scheduler.doNothing() == a.scheduler());
    }

}
