package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.internal.scheduler.SchedulerComputationNonSticky;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;

public class ActorTest {

    private static final Logger log = LoggerFactory.getLogger(ActorTest.class);
    private static final int RUNNERS = 100;
    private static final int NUM_MESSAGES = 10000;

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        Supervisor supervisor = (m, self, error) -> {
            error.printStackTrace();
            self.dispose();
        };
        AtomicBoolean once = new AtomicBoolean();
        ActorRef<Object> a = c //
                .<Object, Integer>match(Integer.class, m -> {
                    m.self().tell("hello");
                }) //
                .match(String.class, m -> {
                    if (once.compareAndSet(false, true)) {
                        m.self().tell(2);
                        latch.countDown();
                    }
                }) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .name("test") //
                .build();
        a.tell(123);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("test", a.toString());
    }

    @Test
    public void testTyped() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = new Context();
        AtomicBoolean once = new AtomicBoolean();
        ActorRef<Number> a = c //
                .<Number, Integer>match(Integer.class, m -> {
                    m.self().tell((Double) 1.2);
                }) //
                .match(Double.class, m -> {
                    if (once.compareAndSet(false, true)) {
                        m.self().tell(2);
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
        Supervisor supervisor = (m, self, error) -> latch.countDown();
        ActorRef<Integer> a = c //
                .match(Integer.class, m -> {
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
        Supervisor supervisor = (m, self, error) -> self.restart();
        ActorRef<Integer> a = c //
                .<Integer>factory(() -> new Actor<Integer>() {

                    @Override
                    public void onMessage(Message<Integer> message) {
                        latch.countDown();
                        throw new RuntimeException("boo");
                    }

                    @Override
                    public void onStop(MessageContext<Integer> context) {
                        // do nothing
                    }

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
        ActorRef<Integer> a = c.<Integer>matchAll(m -> {
            latch.countDown();
            if (m.content() == 1) {
                t[0] = System.currentTimeMillis();
            } else {
                t[1] = System.currentTimeMillis();
            }
            m.self().scheduler().schedule(() -> m.self().tell(2), intervalMs, TimeUnit.MILLISECONDS);
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
                .match(Integer.class, m -> {
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
                .match(Integer.class, m -> {
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
                .match(Integer.class, m -> count.incrementAndGet()) //
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
                .match(Integer.class, m -> {
                    throw new RuntimeException("boo");
                }) //
                .name("thing") //
                .build();
        assertTrue(a == c.<Integer>lookupActor("thing").get());
        a.dispose();
        assertFalse(c.lookupActor("thing").isPresent());
    }

    @Test
    public void testForkJoin() throws InterruptedException, ExecutionException, TimeoutException {
        for (int i = 0; i < Integer.getInteger("fjloops", 1); i++) {
            concurrencyTest(SchedulerForkJoinPool.INSTANCE, RUNNERS,
                    Integer.getInteger("fj", Integer.getInteger("n", NUM_MESSAGES)));
        }
    }

    @Test
    public void testImmediate() throws InterruptedException, TimeoutException, ExecutionException {
        concurrencyTest(Scheduler.immediate(), RUNNERS, Integer.getInteger("n", NUM_MESSAGES));
    }

    @Test
    public void testParallelSticky() throws InterruptedException, ExecutionException, TimeoutException {
        concurrencyTest(Scheduler.computation(), RUNNERS, Integer.getInteger("sticky", NUM_MESSAGES));
    }

    @Test
    public void testParallelNonSticky() throws InterruptedException, ExecutionException, TimeoutException {
        concurrencyTest(SchedulerComputationNonSticky.INSTANCE, RUNNERS, Integer.getInteger("nonsticky", NUM_MESSAGES));
    }

    @Test
    public void testIo() throws InterruptedException, ExecutionException, TimeoutException {
        concurrencyTest(Scheduler.io(), RUNNERS, Integer.getInteger("io", NUM_MESSAGES));
    }

    @Test
    public void testContextShutdownGracefully() throws InterruptedException, ExecutionException, TimeoutException {
        AtomicInteger count = new AtomicInteger();
        Context context = new Context();
        ActorRef<Integer> actor = context.<Integer>matchAll(m -> {
            if (m.content() == 1) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    log.warn(e.getMessage());
                }
            }
            count.incrementAndGet();
        }).build();
        actor.tell(1);
        actor.tell(2);
        actor.tell(3);
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
        actor.tell(4);
        Thread.sleep(100);
        assertEquals(3, count.get());
        Future<Void> future = context.shutdownGracefully();
        future.get(5, TimeUnit.SECONDS);
        assertTrue(future.isDone());
        assertFalse(future.isCancelled());
    }

    @Test
    public void testContextShutdownNow() throws InterruptedException, ExecutionException, TimeoutException {
        AtomicInteger count = new AtomicInteger();
        Context context = new Context();
        ActorRef<Integer> actor = context.<Integer>matchAll(m -> {
            if (m.content() == 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.warn(e.getMessage());
                }
                count.incrementAndGet();
            }
        }).build();
        actor.tell(1);
        actor.tell(2);
        actor.tell(3);
        Thread.sleep(200);
        context.shutdownNow();
        actor.tell(4);
        Thread.sleep(500);
        assertEquals(1, count.get());
    }

    @Test
    public void testDisposeWhileProcessingMessageClearsQueue()
            throws InterruptedException, ExecutionException, TimeoutException {
        AtomicInteger count = new AtomicInteger();
        Context context = new Context();
        ActorRef<Integer> actor = context.<Integer>matchAll(m -> {
            if (m.content() == 1) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    log.warn(e.getMessage());
                }
                count.incrementAndGet();
            }
        }).build();
        actor.tell(1);
        actor.tell(2);
        Thread.sleep(100);
        assertFalse(actor.isDisposed());
        actor.dispose();
        assertTrue(actor.isDisposed());
        Thread.sleep(500);
        assertEquals(1, count.get());
    }

    @Test
    public void testContextShutsDownImmediatelyIfNoActors()
            throws InterruptedException, ExecutionException, TimeoutException {
        Context context = new Context();
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
    }

    @Test(expected = CreateException.class)
    public void testActorCreateAfterContextShutdown()
            throws InterruptedException, ExecutionException, TimeoutException {
        Context context = new Context();
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
        context //
                .matchAll(m -> {
                }) //
                .build();
    }

    private enum Start {
        VALUE;
    }

    // implements the parallel perf test of actr author
    private static void concurrencyTest(Scheduler scheduler, int runners, int messagesPerRunner)
            throws InterruptedException, ExecutionException, TimeoutException {
        log.info("========================================================================");
        log.info(scheduler.getClass().getSimpleName() + ", runners=" + runners + ", messagesPerRunner="
                + messagesPerRunner);
        log.info("========================================================================");
        long t = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1);
        Context context = new Context();
        int[] count = new int[] { runners * messagesPerRunner };
        ActorRef<Object> root = context //
                .<Object, Start>match(Start.class, m -> {
                    for (int i = 0; i < runners; i++) {
                        ActorRef<int[]> actor = m.context() //
                                .<int[]>matchAll(m2 -> {
                                    m2.sender().get().tell(m2.content(), m2.self());
                                }) //
                                .scheduler(scheduler) //
                                .build();
                        for (int j = 0; j < messagesPerRunner; j++) {
                            actor.tell(new int[] { i, j }, m.self());
                        }
                    }
                }) //
                .match(int[].class, m -> {
                    count[0]--;
                    if (count[0] == 0) {
                        latch.countDown();
                    }
                }) //
                .name("root") //
                .scheduler(scheduler) //
                .build();
        assertEquals("root", root.name());
        root.tell(Start.VALUE);
        assertTrue(latch.await(60, TimeUnit.SECONDS));
        context.shutdownGracefully().get(10, TimeUnit.SECONDS);
        log.info("time=" + (System.currentTimeMillis() - t) / 1000.0 + "s");
    }

    @Test
    public void testTennis() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        int maxMessages = 10000;
        Context c = new Context();
        try {
            ActorRef<Integer> b = c.<Integer>matchAll(m -> {
                m.sender().ifPresent(sender -> sender.tell(m.content() + 1));
            }).name("b").build();
            ActorRef<Integer> a = c.<Integer>matchAll(m -> {
                if (m.content() < maxMessages) {
                    b.tell(m.content() + 1, m.self());
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
            c.<Integer>matchAll(m -> {
                m.context().lookupActor("a").get().tell(m.content() + 1);
            }) //
                    .name("b") //
                    .build();
            c.<Integer>matchAll(m -> {
                if (m.content() < maxMessages) {
                    m.context().lookupActor("b").get().tell(m.content() + 1, m.self());
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

    @Test
    public void testDisposeParentDisposesChild() {
        Context context = new Context();
        AtomicBoolean called = new AtomicBoolean();
        ActorRef<Object> a = context.matchAll(m -> {
        }).build();
        ActorRef<Object> b = context.matchAll(m -> called.set(true)).scheduler(Scheduler.immediate()).parent(a).build();
        a.dispose();
        b.tell(1);
        assertFalse(called.get());
    }

    @Test
    public void testStopParentStopsChild() {
        Context context = new Context();
        AtomicBoolean called = new AtomicBoolean();
        ActorRef<Object> a = context.matchAll(m -> {
        }) //
                .scheduler(Scheduler.immediate()) //
                .build();
        ActorRef<Object> b = context //
                .matchAll(m -> called.set(true)) //
                .scheduler(Scheduler.immediate()) //
                .parent(a) //
                .build();
        a.stop();
        b.tell(1);
        assertFalse(called.get());
    }

    @Test
    public void testAsk() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = new Context();
        ActorRef<String> actor = context.<String>matchAll(m -> m.sender().ifPresent(sender -> sender.tell("boo"))) //
                .build();
        assertEquals("boo", actor.ask("hi").get(1000, TimeUnit.MILLISECONDS));
        context.dispose();
    }

    @Test
    public void testCreateAndStop() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = new Context((c, actor, error) -> {
            log.error(actor.name() + ":" + error.getMessage(), error);
        }, //
                () -> new ActorDoNothing<Object>());
        context //
                .matchAll(m -> m.self().stop()) //
                .scheduler(Scheduler.immediate()) //
                .build() //
                .tell(Boolean.TRUE);
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testAs() {
        Context context = new Context();
        ActorRef<Number> a = context.<Number>matchAll(m -> {
        }) //
                .build();
        @SuppressWarnings("unused")
        ActorRef<Integer> b = a.narrow();
        @SuppressWarnings("unused")
        ActorRef<Integer> c = a.recast();
    }

    @Test
    public void testMatchEquals() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = new Context();
        CountDownLatch latch = new CountDownLatch(1);
        ActorRef<Integer> a = context //
                .matchEquals(1, m -> m.self().tell(2)) //
                .matchEquals(2, m -> m.self().tell(3)) //
                .matchEquals(3, m -> latch.countDown()) //
                .build();
        a.tell(0);
        a.tell(1);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
    }

    public static final class MyActor implements Actor<Integer> {

        static volatile Integer last;

        @Override
        public void onMessage(Message<Integer> message) {
            last = message.content();
        }

        @Override
        public void onStop(MessageContext<Integer> context) {

        }

    }

    public static final class MyActorBad implements Actor<Integer> {

        public MyActorBad(String s) {

        }

        @Override
        public void onMessage(Message<Integer> message) {
        }

        @Override
        public void onStop(MessageContext<Integer> context) {

        }
    }

}
