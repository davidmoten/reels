package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Ignore;
import org.junit.Test;

public class ActorTest {

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        Supervisor supervisor = new Supervisor() {
            @Override
            public void processFailure(Context context, ActorRef<?> actorRef, Throwable error) {
                error.printStackTrace();
                actorRef.dispose();
            }
        };
        AtomicBoolean once = new AtomicBoolean();
        ActorRef<Object> a = c //
                .messageClass(Object.class) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .match(Integer.class, (ctxt, n) -> {
                    ctxt.self().tell("hello");
                }) //
                .match(String.class, (ctxt, s) -> {
                    if (once.compareAndSet(false, true)) {
                        ctxt.self().tell(2);
                        latch.countDown();
                    }
                }) //
                .name("test") //
                .build();
        a.tell(123);
        latch.await();
    }

    @Test
    public void testTyped() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        AtomicBoolean once = new AtomicBoolean();
        ActorRef<Number> a = c //
                .messageClass(Number.class) //
                .scheduler(Scheduler.computation()) //
                .match(Integer.class, (ctxt, n) -> {
                    ctxt.self().tell((Double) 1.2);
                }) //
                .match(Double.class, (ctxt, s) -> {
                    if (once.compareAndSet(false, true)) {
                        ctxt.self().tell(2);
                        latch.countDown();
                    }
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
//    @Ignore
    public void testParallel() throws InterruptedException {
            System.out.println("=================================================");
            long t = System.currentTimeMillis();
            String start = "start";
            Context c = new Context();
            int runners = 100;
            int messagesPerRunner = 10000;
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger count = new AtomicInteger();
            int[] countFinished = new int[1]; 
            ActorRef<String> root = c.messageClass(String.class) //
                    .name("root") //
                    .match(String.class, (con1, msg) -> {
                        if (start.equals(msg)) {
                            for (int i = 1; i <= runners; i++) {
                                int finalI = i;
                                ActorRef<String> r = c.messageClass(String.class) //
                                        .name("runner" + i) //
                                        .match(String.class, (con2, m) -> {
//                                        DecimalFormat df = new DecimalFormat("000");
//                                        System.out.println("responding from runner " + finalI + " with value " + m);
//                                          String reply = "reply from runner " + df.format(finalI) + " to message " + m;
                                            int n = count.incrementAndGet();
                                            if (n % 100000 == 0) {
                                                System.out.println("runner received count = " + n);
                                            }
                                            con2.sender().get().tell("reply");
                                        }).build();
                                for (int j = 1; j <= messagesPerRunner; j++) {
//                              System.out.println("sending runner " + i + ": " + j);
                                    r.tell(j + "", con1.self());
                                }
                            }
                        } else {
                            long n = ++countFinished[0];
                            if (n % 100000 == 0) {
                                System.out.println(n);
                            } 
                            if (n == runners * messagesPerRunner) {
                                latch.countDown();
                            }
//                        System.out.println(msg + ", replies=" + count.incrementAndGet());
                        }
                    }).build();
            root.tell(start);
            if (!latch.await(60, TimeUnit.SECONDS)) {
                org.junit.Assert.fail();
            }
        System.out.println("time=" + (System.currentTimeMillis() - t)/1000.0 + "s");
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
