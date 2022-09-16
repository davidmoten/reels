package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.internal.scheduler.SchedulerComputationNonSticky;

public class ActorTest {

    private static final Logger log = LoggerFactory.getLogger(ActorTest.class);

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        Supervisor supervisor = (context, actor, error) -> {
            error.printStackTrace();
            actor.dispose();
        };
        AtomicBoolean once = new AtomicBoolean();
        ActorRef<Object> a = c //
                .<Object, Integer>match(Integer.class, (ctxt, n) -> {
                    ctxt.self().tell("hello");
                }) //
                .match(String.class, (ctxt, s) -> {
                    if (once.compareAndSet(false, true)) {
                        ctxt.self().tell(2);
                        latch.countDown();
                    }
                }) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .name("test") //
                .build();
        a.tell(123);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testTyped() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        AtomicBoolean once = new AtomicBoolean();
        ActorRef<Number> a = c //
                .<Number, Integer>match(Integer.class, (ctxt, n) -> {
                    ctxt.self().tell((Double) 1.2);
                }) //
                .match(Double.class, (ctxt, s) -> {
                    if (once.compareAndSet(false, true)) {
                        ctxt.self().tell(2);
                        latch.countDown();
                    }
                }).scheduler(Scheduler.computation()) //
                .build();
        a.tell(123);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testSupervisorCalled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        Supervisor supervisor = (context, actor, error) -> latch.countDown();
        ActorRef<Integer> a = c //
                .match(Integer.class, (ctxt, n) -> {
                    throw new RuntimeException("boo");
                }) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .build();
        a.tell(123);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
    }

    @Test
    public void testSupervisorCreatesAgainOnRestart() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        Context c = new Context();
        Supervisor supervisor = (context, actor, error) -> actor.restart();
        ActorRef<Integer> a = c //
                .<Integer>factory(() -> {
                    latch.countDown();
                    return (context, message) -> {
                        throw new RuntimeException("boo");
                    };
                }) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .build();
        a.tell(123);
        a.tell(345);
        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test
    public void testScheduledMessage() throws InterruptedException {
        Context c = Context.create();
        CountDownLatch latch = new CountDownLatch(2);
        long[] t = new long[2];
        long intervalMs = 300;
        ActorRef<Integer> a = c.<Integer>processor((ctxt, message) -> {
            latch.countDown();
            if (message == 1) {
                t[0] = System.currentTimeMillis();
            } else {
                t[1] = System.currentTimeMillis();
            }
            ctxt.self().worker().schedule(() -> ctxt.self().tell(2), intervalMs, TimeUnit.MILLISECONDS);
        }).build();
        a.tell(1);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertTrue(t[1] - t[0] > intervalMs - 100);
    }

    @Test
    public void testSupervisorDisposesActor() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        Supervisor supervisor = (context, actor, error) -> {
            actor.dispose();
            latch.countDown();
        };
        AtomicInteger count = new AtomicInteger();
        ActorRef<Integer> a = c //
                .match(Integer.class, (ctxt, n) -> {
                    count.incrementAndGet();
                    throw new RuntimeException("boo");
                }) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .build();
        a.tell(123);
        a.tell(234);
        a.tell(456);
        Thread.sleep(1000);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        a.tell(999);
        assertEquals(1, count.get());
    }

    @Test
    public void testBuilderOnError() throws InterruptedException {
        AtomicBoolean supervisorCalled = new AtomicBoolean();
        Supervisor supervisor = (context, actor, error) -> supervisorCalled.set(true);
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        ActorRef<Integer> a = c //
                .match(Integer.class, (ctxt, n) -> {
                    throw new RuntimeException("boo");
                }) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .onError(e -> {
                    if ("boo".equals(e.getMessage())) {
                        latch.countDown();
                    }
                }).build();
        a.tell(123);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        Thread.sleep(200);
        assertFalse(supervisorCalled.get());
    }

    @Test
    public void testCustomActorWithoutBuilder() throws InterruptedException {
        Context c = new Context();
        ActorRef<Integer> a = c.createActor(MyActor.class);
        a.tell(123);
        Thread.sleep(500);
        assertEquals(123, (int) MyActor.last);
    }

    @Test(expected = CreateException.class)
    public void testCustomActorWithoutBuilderNoPublicNoArgConstructor() throws InterruptedException {
        Context c = new Context();
        c.createActor(MyActorBad.class);
    }

    @Test
    public void testKill() throws InterruptedException {
        Context context = new Context();
        AtomicInteger count = new AtomicInteger();
        ActorRef<Integer> a = context //
                .match(Integer.class, //
                        (c, msg) -> count.incrementAndGet()) //
                .build();
        a.tell(1);
        a.stop();
        a.tell(2);
        Thread.sleep(500);
        assertEquals(1, count.get());
    }

    @Test
    public void testLookup() {
        Context c = new Context();
        ActorRef<Integer> a = c //
                .match(Integer.class, (ctxt, n) -> {
                    throw new RuntimeException("boo");
                }) //
                .name("thing") //
                .build();
        assertTrue(a == c.<Integer>lookupActor("thing").get());
        a.dispose();
        assertFalse(c.lookupActor("thing").isPresent());
    }

    @Test(timeout = 300000)
    public void testParallel() throws InterruptedException {
        concurrencyTest(Scheduler.computation(), Integer.getInteger("n", 10000));
    }
    
    @Test(timeout = 300000)
    public void testParallelNonSticky() throws InterruptedException {
        concurrencyTest(SchedulerComputationNonSticky.INSTANCE, Integer.getInteger("n", 10000));
    }

    @Test(timeout = 30000)
    public void testImmediate() throws InterruptedException {
        concurrencyTest(Scheduler.immediate(), 10000);
    }

    @Test(timeout = 30000)
    public void testIo() throws InterruptedException {
        concurrencyTest(Scheduler.io(), 10000);
    }

    private static void concurrencyTest(Scheduler scheduler, int messagesPerRunner)  throws InterruptedException {
        log.info("=================================================");
        log.info("" + scheduler.getClass().getSimpleName());
        log.info("=================================================");
        long t = System.currentTimeMillis();
        String start = "start";
        Context c = new Context();
        try {
            int runners = 100;
            CountDownLatch latch = new CountDownLatch(1);
            AtomicInteger count = new AtomicInteger();
            int[] countFinished = new int[1];
            ActorRef<String> root = c.<String>builder() //
                    .name("root") //
                    .scheduler(scheduler) //
                    .match(String.class, (con1, msg) -> {
                        if (start.equals(msg)) {
                            for (int i = 1; i <= runners; i++) {
//                                int finalI = i;
                                ActorRef<String> r = c.<String>builder() //
                                        .name("runner" + i) //
                                        .scheduler(scheduler) //
                                        .parent(con1.self()) //
                                        .match(String.class, (con2, m) -> {
//                                    DecimalFormat df = new DecimalFormat("000");
//                                    log.info("responding from runner " + finalI + " with value " + m);
//                                      String reply = "reply from runner " + df.format(finalI) + " to message " + m;
                                            int n = count.incrementAndGet();
                                            if (n % 100000 == 0) {
                                                log.info("runner received count = " + n);
                                            }
                                            con2.sender().get().tell("reply");
//                                        con2.self().kill();
                                        }).build();
                                for (int j = 1; j <= messagesPerRunner; j++) {
//                          log.info("sending runner " + i + ": " + j);
                                    r.tell(j + "", con1.self());
                                }
                            }
                        } else {
                            long n = ++countFinished[0];
                            if (n % 100000 == 0) {
                                log.info("" + n);
                            }
                            if (n == runners * messagesPerRunner) {
                                latch.countDown();
                            }
//                    log.info(msg + ", replies=" + count.incrementAndGet());
                        }
                    }).build();
            root.tell(start);
            if (!latch.await(60, TimeUnit.SECONDS)) {
                org.junit.Assert.fail();
            }
        } finally {
            c.dispose();
        }
        log.info("time=" + (System.currentTimeMillis() - t) / 1000.0 + "s");
    }

    @Test
    public void testTennis() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        int maxMessages = 10000;
        Context c = new Context();
        try {
            ActorRef<Integer> b = c.<Integer>processor((ctxt, message) -> {
                ctxt.sender().ifPresent(sender -> sender.tell(message + 1));
            }).name("b").build();
            ActorRef<Integer> a = c.<Integer>processor((ctxt, message) -> {
                if (message < maxMessages) {
                    b.tell(message + 1, ctxt.self());
                } else {
                    latch.countDown();
                }
            }).name("a").build();
            a.tell(0);
            assertTrue(latch.await(10, TimeUnit.SECONDS));
        } finally {
            c.dispose();
        }
    }

    @Test
    public void testTennisByLookup() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        int maxMessages = 100;
        Context c = new Context();
        try {
            c.<Integer>processor((ctxt, message) -> {
                ctxt.context().lookupActor("a").get().tell(message + 1);
            }) //
                    .name("b") //
                    .build();
            c.<Integer>processor((ctxt, message) -> {
                if (message < maxMessages) {
                    ctxt.context().lookupActor("b").get().tell(message + 1, ctxt.self());
                } else {
                    latch.countDown();
                }
            }) //
                    .name("a") //
                    .build();
            c.lookupActor("a").get().tell(0);
            latch.await(60, TimeUnit.SECONDS);
        } finally {
            c.dispose();
        }
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
