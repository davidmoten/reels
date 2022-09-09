package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
                actorRef.dispose();
            }
        };
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
                }).build();
        a.tell(123);
        latch.await();
    }

    @Test
    public void testTyped() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        Context c = new Context();
        ActorRef<Number> a = c //
                .messageClass(Number.class) //
                .scheduler(Scheduler.computation()) //
                .match(Integer.class, (ctxt, n) -> {
                    ctxt.self().tell((Double) 1.2);
                }) //
                .match(Double.class, (ctxt, s) -> {
                    ctxt.self().tell(2);
                    latch.countDown();
                }).build();
        a.tell(123);
        latch.await();
    }

    @Test
    public void testSupervisorCalled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        Supervisor supervisor = new Supervisor() {
            @Override
            public void processFailure(Context context, ActorRef<?> actorRef, Throwable error) {
                latch.countDown();
            }
        };
        ActorRef<Integer> a = c //
                .messageClass(Integer.class) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .match(Integer.class, (ctxt, n) -> {
                    throw new RuntimeException("boo");
                }) //
                .build();
        a.tell(123);
        latch.await();
    }

    @Test
    public void testSupervisorDisposesActor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        Supervisor supervisor = new Supervisor() {
            @Override
            public void processFailure(Context context, ActorRef<?> actor, Throwable error) {
                actor.dispose();
                System.out.println("disposed actor");
                latch.countDown();          }
        };
        AtomicInteger count = new AtomicInteger();
        ActorRef<Integer> a = c //
                .messageClass(Integer.class) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .match(Integer.class, (ctxt, n) -> {
                    count.incrementAndGet();
                    throw new RuntimeException("boo");
                }) //
                .build();
        a.tell(123);
        a.tell(234);
        a.tell(456);
        Thread.sleep(1000);
        latch.await();
        a.tell(999);
        assertEquals(1, count.get());
    }

}
