package com.github.davidmoten.reels.internal.supervisor;

import org.junit.Test;
import org.mockito.Mockito;

import com.github.davidmoten.reels.Message;
import com.github.davidmoten.reels.SupervisedActorRef;

public class SupervisorDefaultTest {

    @Test
    public void test() {
        @SuppressWarnings("unchecked")
        Message<Object> m = Mockito.mock(Message.class);
        @SuppressWarnings("unchecked")
        SupervisedActorRef<Object> actor = Mockito.mock(SupervisedActorRef.class);
        Mockito.when(actor.name()).thenReturn("Anonymous-1");
        RuntimeException e = new RuntimeException("boo");
        SupervisorDefault.INSTANCE.processFailure(m, actor, e);
        Mockito.verify(actor, Mockito.times(1)).dispose();
        Mockito.verify(actor, Mockito.atLeastOnce()).name();
        Mockito.verifyNoMoreInteractions(actor);
        Mockito.verifyNoMoreInteractions(m);
    }

}
