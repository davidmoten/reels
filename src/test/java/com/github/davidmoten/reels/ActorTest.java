package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        Thread.sleep(500);
        assertEquals(123, (int) MyActor.last);
    }

    @Test(expected = CreateException.class)
    public void testCustomActorWithoutBuilderNoPublicNoArgConstructor() throws InterruptedException {
        Context c = new Context();
        c.create(MyActorBad.class);
    }

    @Test
    public void testLookup() {
        Context c = new Context();
        ActorRef<Integer> a = c //
                .messageClass(Integer.class) //
                .match(Integer.class, (ctxt, n) -> {
                    throw new RuntimeException("boo");
                }) //
                .name("thing") //
                .build();
        assertTrue(a == c.<Integer>lookupActor("thing").get());
        a.dispose();
        assertFalse(c.lookupActor("thing").isPresent());
    }

    @Test
    public void testParallel() throws InterruptedException {
        Integer msg = 0;
        Context c = new Context();
        int runners = 100;
        int messagesPerRunner = 5;
        CountDownLatch latch = new CountDownLatch(runners * messagesPerRunner);
        AtomicInteger count = new AtomicInteger();
        ActorRef<Integer> root = c.messageClass(Integer.class) //
                .name("root") //
                .match(Integer.class, (con1, n) -> {
                    if (n == msg) {
                        for (int i = 1; i <= runners; i++) {
                            int finalI = i;
                            ActorRef<Integer> r = c.messageClass(Integer.class) //
                                    .name("runner" + i) //
                                    .match(Integer.class, (con2, m) -> {
                                        System.out.println("responding from runner " + finalI + " with value " + m);
                                        con2.sender().get().tell(m);
                                    }).build();
                            for (int j = 1; j <= messagesPerRunner; j++) {
                                System.out.println("sending runner " + i + ": " + j);
                                r.tell(j, con1.self());
                            }
                        }
                    } else {
                        latch.countDown();
                        System.out.println("replies=" + count.incrementAndGet());
                    }
                }).build();
        root.tell(msg);
        Thread.sleep(5000);
        latch.await();
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
