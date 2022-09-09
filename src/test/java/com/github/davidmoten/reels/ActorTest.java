package com.github.davidmoten.reels;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class ActorTest {

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        Context c = new Context();
        Supervisor supervisor = new Supervisor() {
            @Override
            public void processFailure(Context context, ActorRef<?> actorRef, Throwable error) {
                error.printStackTrace();
            }};
        ActorRef<Object> a = c //
                .messageClass(Object.class) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .match(Integer.class, (ctxt, n) -> {
                    ctxt.self().tell("hello");
                }) //
                .match(String.class, (ctxt, s) -> {
                    ctxt.self().tell(2);
                    latch.countDown();                    
                })
                .build();
        a.tell(123);
        latch.await();
    }

}
