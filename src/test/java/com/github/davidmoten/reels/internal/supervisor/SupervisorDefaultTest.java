package com.github.davidmoten.reels.internal.supervisor;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.SupervisedActorRef;
import com.github.davidmoten.reels.internal.util.StdOut;

public class SupervisorDefaultTest {

    @Test
    public void test() throws Exception {
        try (StdOut out = StdOut.create()) {
            @SuppressWarnings("unchecked")
            Message<Object> m = Mockito.mock(Message.class);
            @SuppressWarnings("unchecked")
            SupervisedActorRef<Object> actor = Mockito.mock(SupervisedActorRef.class);
            Mockito.when(actor.name()).thenReturn("Anonymous-1");
            RuntimeException e = new RuntimeException("boo");
            SupervisorDefault.INSTANCE.processFailure(m, actor, e);
            Mockito.verify(actor, Mockito.times(1)).stopNow();
            Mockito.verify(actor, Mockito.atLeastOnce()).name();
            Mockito.verifyNoMoreInteractions(actor);
            Mockito.verifyNoMoreInteractions(m);
            assertTrue(out.text().contains("RuntimeException: boo"));
        }
    }

}
