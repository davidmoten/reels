package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
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
                }) //
                .name("test") //
                .build();
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
                latch.countDown();
            }
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

    @Test
    public void testBuilderOnError() throws InterruptedException {
        AtomicBoolean supervisorCalled = new AtomicBoolean();
        Supervisor supervisor = new Supervisor() {
            @Override
            public void processFailure(Context context, ActorRef<?> actor, Throwable error) {
                supervisorCalled.set(true);
            }
        };
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        ActorRef<Integer> a = c //
                .messageClass(Integer.class) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .match(Integer.class, (ctxt, n) -> {
                    throw new RuntimeException("boo");
                }) //
                .onError(e -> {
                    if ("boo".equals(e.getMessage())) {
                        latch.countDown();
                    }
                }).build();
        a.tell(123);
        latch.await();
        Thread.sleep(200);
        assertFalse(supervisorCalled.get());
    }

    @Test
    public void testCustomActorWithoutBuilder() throws InterruptedException {
        Context c = new Context();
        ActorRef<Integer> a = c.create(MyActor.class);
        a.tell(123);
        Thread.sleep(200);
        assertEquals(123, (int) MyActor.last);
    }

    @Test(expected = CreateException.class)
    public void testCustomActorWithoutBuilderNoPublicNoArgConstructor() throws InterruptedException {
        Context c = new Context();
        c.create(MyActorBad.class);
    }

    public static final class MyActor implements Actor<Integer> {

        static volatile Integer last;

        @Override
        public void onMessage(MessageContext<Integer> context, Integer message) {
            last = message;
        }
    }

    public static final class MyActorBad implements Actor<Integer> {

        public MyActorBad(String s) {

        }

        @Override
        public void onMessage(MessageContext<Integer> context, Integer message) {
        }
    }
}
