package com.github.davidmoten.reels;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class ActorTest {

    @Test
    public void test() throws InterruptedException {
        Context c = new Context();
        CountDownLatch latch = new CountDownLatch(1);
        ActorRef<Object> a = c //
                .messageClass(Object.class) //
                .match(Integer.class, (ctxt, n) -> latch.countDown())
                .build();
        a.tell(123);
        latch.await();
    }

}
