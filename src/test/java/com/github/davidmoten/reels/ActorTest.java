package com.github.davidmoten.reels;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class ActorTest {

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        Context c = new Context();
        ActorRef<Object> a = c //
                .messageClass(Object.class) //
                .match(Integer.class, (ctxt, n) -> {
                    ctxt.self().tell(2);
                    latch.countDown();
                }) //
                .build();
        a.tell(123);
        latch.await();
    }

}
