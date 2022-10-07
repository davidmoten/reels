package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.davidmoten.reels.internal.scheduler.SchedulerComputationNonSticky;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;
import com.github.davidmoten.reels.internal.scheduler.TestScheduler;

public class ActorTest {

    private static final Logger log = LoggerFactory.getLogger(ActorTest.class);
    private static final int RUNNERS = 100;
    private static final int NUM_MESSAGES = 10000;

    @Test
    public void test() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = Context.create();
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
                .supervisor(Supervisor.defaultSupervisor()) //
                .name("test") //
                .build();
        a.tell(123);
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals("test", a.toString());
        assertEquals(Supervisor.defaultSupervisor(), c.supervisor());
    }

    @Test
    public void testPreStartCalled() throws InterruptedException, ExecutionException, TimeoutException {
        Context c = Context.create();
        List<String> list = new ArrayList<>();
        ActorRef<Object> a = c.matchAny(m -> list.add("message")) //
                .preStart(context -> list.add("preStart")) //
                .onStop(context -> list.add("onStop")) //
                .scheduler(Scheduler.immediate()) //
                .supervisor(Supervisor.defaultSupervisor()) //
                .name("test") //
                .build();
        a.tell(123);
        a.tell(456);
        a.stop();
        // will send another stop so we are testing that onStop gets called only once
        c.shutdownGracefully().get(5, TimeUnit.SECONDS);
        assertEquals(Arrays.asList("preStart", "message", "message", "onStop"), list);
    }

    @Test
    public void testPreStartThrows() throws InterruptedException, ExecutionException, TimeoutException {
        Context c = Context.create();
        List<String> list = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        ActorRef<Object> a = c.matchAny(m -> list.add("message")) //
                .preStart(context -> {
                    throw new RuntimeException("boo");
                }) //
                .scheduler(Scheduler.immediate()) //
                .supervisor((m, self, error) -> {
                    err.set(error);
                    latch.countDown();
                }) //
                .name("test") //
                .build();
        a.tell(123);
        latch.await(5, TimeUnit.SECONDS);
        assertTrue(err.get() instanceof PreStartException);
        assertEquals("boo", err.get().getCause().getMessage());
        // will send another stop so we are testing that onStop gets called only once
        c.shutdownGracefully().get(5, TimeUnit.SECONDS);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryPresentWhenMatchAnyCalled() {
        Context c = new Context();
        c.factory(() -> new MyActor()) //
                .matchAny(m -> {
                });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryPresentWhenMatchEqualsCalled() {
        Context c = new Context();
        c.factory(() -> new MyActor()) //
                .matchEquals(1, m -> {
                });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFactoryPresentWhenMatchCalled() {
        Context c = new Context();
        c.factory(() -> new MyActor()) //
                .match(Integer.class, m -> {
                });
    }

    @Test
    public void testActorClass() {
        Context c = new Context();
        ActorRef<Integer> a = c.<Integer>actorBuilder().actorClass(MyActor.class).scheduler(Scheduler.immediate())
                .build();
        a.tell(1234);
        assertEquals(1234, (int) MyActor.last);
    }

    @Test
    public void testTyped() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Context c = Context.create();
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
        Context c = Context.create();
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
    public void onStopCalled() {
        Context c = Context.create();
        AtomicBoolean called = new AtomicBoolean();
        ActorRef<Object> a = c.matchAny(m -> {
        }).onStop(m -> called.set(true)).scheduler(Scheduler.immediate()).build();
        assertFalse(called.get());
        a.stop();
        assertTrue(called.get());
        c.dispose();
    }

    @Test
    public void testSupervisorCreatesAgainOnRestart() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        Context c = Context.create();
        Supervisor supervisor = (m, self, error) -> self.restart();
        ActorRef<Integer> a = c //
                .<Integer>factory(() -> new AbstractActor<Integer>() {

                    @Override
                    public void onMessage(Message<Integer> message) {
                        latch.countDown();
                        throw new RuntimeException("boo");
                    }

                }) //
                .scheduler(Scheduler.computation()) //
                .supervisor(supervisor) //
                .build();
        a.tell(123);
        a.tell(345);
        assertTrue(latch.await(30, TimeUnit.SECONDS));
    }

    @Test(expected = CreateException.class)
    public void testFactoryReturnsNull() {
        Context c = Context.create();
        c.factory(() -> null).build();
    }

    @Test
    public void testAddChildToDisposedParentWillDisposeChild() {
        Context c = Context.create();
        ActorRef<Object> a = c.createActor(ActorDoNothing::create);
        a.dispose();
        ActorRef<Object> b = c.matchAny(m -> {
        }).parent(a).build();
        assertTrue(a == b.parent());
        assertTrue(b.isDisposed());
    }

    @Test
    public void testAskFuture() throws InterruptedException, ExecutionException, TimeoutException {
        Context c = Context.create();
        CountDownLatch latch = new CountDownLatch(1);
        ActorRef<Object> a = c.matchAny(m -> {
            try {
                latch.await(5, TimeUnit.SECONDS);
                m.senderRaw().tell(2);
            } catch (InterruptedException e) {
                // do nothing
            }
        }).build();
        CompletableFuture<Integer> f = a.ask(1);
        assertFalse(f.isCancelled());
        assertFalse(f.isDone());
        f.cancel(true);
        assertTrue(f.isCancelled());
        f.cancel(true);
        assertTrue(f.isCancelled());
        latch.countDown();
        c.shutdownGracefully().get(5, TimeUnit.SECONDS);
        assertTrue(f.isCompletedExceptionally());
    }

    @Test
    public void testScheduledMessage() throws InterruptedException {
        Context c = Context.create();
        CountDownLatch latch = new CountDownLatch(2);
        long[] t = new long[2];
        long intervalMs = 300;
        ActorRef<Integer> a = c.<Integer>matchAny(m -> {
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
        Context c = Context.create();
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
    public void testSupervisorRetriesAndRestartsAfterDelay()
            throws InterruptedException, ExecutionException, TimeoutException {
        Context c = Context.create();
        Supervisor supervisor = (context, actor, error) -> {
            actor.retry();
            actor.pauseAndRestart(1, TimeUnit.MILLISECONDS);
        };
        AtomicBoolean once = new AtomicBoolean();
        AtomicInteger count = new AtomicInteger();
        ActorRef<Integer> a = c //
                .match(Integer.class, m -> {
                    count.incrementAndGet();
                    if (once.compareAndSet(false, true)) {
                        throw new RuntimeException("boo");
                    }
                }) //
                .scheduler(Scheduler.immediate()) //
                .supervisor(supervisor) //
                .build();
        a.tell(123);
        assertEquals(2, count.get());
        c.shutdownGracefully().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testSupervisorRetriesAndRestarts() throws InterruptedException, ExecutionException, TimeoutException {
        Context c = Context.create();
        Supervisor supervisor = (context, actor, error) -> {
            actor.retry();
            actor.pauseAndRestart(1, TimeUnit.MILLISECONDS);
        };
        AtomicBoolean once = new AtomicBoolean();
        AtomicInteger count = new AtomicInteger();
        ActorRef<Integer> a = c //
                .match(Integer.class, m -> {
                    count.incrementAndGet();
                    if (once.compareAndSet(false, true)) {
                        throw new RuntimeException("boo");
                    }
                }) //
                .scheduler(Scheduler.immediate()) //
                .supervisor(supervisor) //
                .build();
        a.tell(123);
        assertEquals(2, count.get());
        c.shutdownGracefully().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testBuilderOnError() throws InterruptedException {
        AtomicBoolean supervisorCalled = new AtomicBoolean();
        Supervisor supervisor = (context, actor, error) -> supervisorCalled.set(true);
        CountDownLatch latch = new CountDownLatch(1);
        Context c = Context.create();
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
        Context c = Context.create();
        ActorRef<Integer> a = c.createActor(MyActor.class);
        a.tell(123);
        Thread.sleep(500);
        assertEquals(123, (int) MyActor.last);
    }

    @Test(expected = CreateException.class)
    public void testCustomActorWithoutBuilderNoPublicNoArgConstructor() throws InterruptedException {
        Context c = Context.create();
        c.createActor(MyActorBad.class);
    }

    @Test
    public void testKill() throws InterruptedException {
        Context context = Context.create();
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
        Context c = Context.create();
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
        Context context = Context.create();
        ActorRef<Integer> actor = context.<Integer>matchAny(m -> {
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
        Context context = Context.create();
        ActorRef<Integer> actor = context.<Integer>matchAny(m -> {
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
        Context context = Context.create();
        ActorRef<Integer> actor = context.<Integer>matchAny(m -> {
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
        Context context = Context.create();
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
    }

    @Test(expected = CreateException.class)
    public void testActorCreateAfterContextShutdown()
            throws InterruptedException, ExecutionException, TimeoutException {
        Context context = Context.create();
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
        context //
                .matchAny(m -> {
                }) //
                .build();
    }

    @Test
    public void testDisposeTwice() {
        Context context = Context.create();
        assertFalse(context.isDisposed());
        context.dispose();
        assertTrue(context.isDisposed());
        context.dispose();
        assertTrue(context.isDisposed());
    }

    @Test(expected = ExecutionException.class)
    public void testShutdownGracefullyAfterDispose() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = Context.create();
        context.dispose();
        context.shutdownGracefully().get(1, TimeUnit.SECONDS);
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
        Context context = Context.create();
        int[] count = new int[] { runners * messagesPerRunner };
        ActorRef<Object> root = context //
                .<Object, Start>match(Start.class, m -> {
                    for (int i = 0; i < runners; i++) {
                        ActorRef<int[]> actor = m.context() //
                                .<int[]>matchAny(m2 -> {
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
        Context c = Context.create();
        try {
            ActorRef<Integer> b = c.<Integer>matchAny(m -> {
                m.sender().ifPresent(sender -> sender.tell(m.content() + 1));
            }).name("b").build();
            ActorRef<Integer> a = c.<Integer>matchAny(m -> {
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
        Context c = Context.create();
        try {
            c.<Integer>matchAny(m -> {
                m.context().lookupActor("a").get().tell(m.content() + 1);
            }) //
                    .name("b") //
                    .build();
            c.<Integer>matchAny(m -> {
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
        Context context = Context.create();
        AtomicBoolean called = new AtomicBoolean();
        ActorRef<Object> a = context.matchAny(m -> {
        }).build();
        ActorRef<Object> b = context.matchAny(m -> called.set(true)).scheduler(Scheduler.immediate()).parent(a).build();
        a.dispose();
        b.tell(1);
        assertFalse(called.get());
    }

    @Test
    public void testStopParentStopsChild() {
        Context context = Context.create();
        AtomicBoolean called = new AtomicBoolean();
        ActorRef<Object> a = context.matchAny(m -> {
        }) //
                .scheduler(Scheduler.immediate()) //
                .build();
        ActorRef<Object> b = context //
                .matchAny(m -> called.set(true)) //
                .scheduler(Scheduler.immediate()) //
                .parent(a) //
                .build();
        a.stop();
        b.tell(1);
        assertFalse(called.get());
    }

    @Test
    public void testAsk() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = Context.create();
        ActorRef<String> actor = context.<String>matchAny(m -> m.sender().ifPresent(sender -> sender.tell("boo"))) //
                .build();
        assertEquals("boo", actor.ask("hi").get(1000, TimeUnit.MILLISECONDS));
        context.dispose();
    }

    @Test
    public void testCreateAndStop() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = Context //
                .builder() //
                .supervisor((c, actor, error) -> log.error(actor.name() + ":" + error.getMessage(), error)) //
                .deadLetterActorFactory(ActorDoNothing::create) //
                .build();
        context //
                .matchAny(m -> m.self().stop()) //
                .scheduler(Scheduler.immediate()) //
                .build() //
                .tell(Boolean.TRUE);
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
    }

    @Test
    public void testOnStopThrows() throws InterruptedException, ExecutionException, TimeoutException {
        AtomicReference<Throwable> e = new AtomicReference<>();
        Context context = Context.builder().supervisor((m, self, error) -> e.set(error)).build();
        context //
                .matchAny(m -> m.self().stop()) //
                .onStop(c -> {
                    throw new IllegalArgumentException("boo");
                }) //
                .scheduler(Scheduler.immediate()) //
                .build() //
                .tell(Boolean.TRUE);
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
        assertTrue(e.get() != null);
        assertTrue(e.get() instanceof OnStopException);
        assertEquals("boo", e.get().getCause().getMessage());
    }

    @Test
    public void testAs() {
        Context context = Context.create();
        ActorRef<Number> a = context.<Number>matchAny(m -> {
        }) //
                .build();
        @SuppressWarnings("unused")
        ActorRef<Integer> b = a.narrow();
        @SuppressWarnings("unused")
        ActorRef<Integer> c = a.recast();
    }

    @Test
    public void createActorKitchenSink() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = Context.create();
        ActorRef<Number> a = context //
                .<Number>matchAny(m -> {
                    log.info("{}: parent received {}", m.self(), m.content());
                    m.self().child("b").tell(m.content(), m.self());
                }) //
                .name("a") //
                .scheduler(Scheduler.single()) //
                .onStop(self -> log.info("{}: onStop", self)) //
                .build();
        context //
                .<Number>matchEquals(1, m -> {
                    log.info("{}: equal matched, sender = {}", m.self(), m.sender());
                    m.sender().ifPresent(x -> x.tell(9999));
                }) //
                .match(Integer.class, m -> log.info("{}: received integer {}", m.self(), m.content())) //
                .match(Double.class, m -> log.info("{}: received double {}", m.self(), m.content())) //
                .matchAny(m -> log.info("{}: received something else {}", m.self(), m.content())) //
                .name("b") //
                .onError(e -> e.printStackTrace()) //
                .preStart(self -> log.info("{}: preStart", self)) //
                .onStop(self -> log.info("{}: onStop", self)) //
                .scheduler(Scheduler.computation()) //
                .parent(a) //
                .supervisor((m, actor, e) -> {
                    log.error(e.getMessage(), e);
                    actor.pause(30, TimeUnit.SECONDS);
                    actor.retry();
                }) //
                .build();
        a.tell(1);
        a.tell(2);
        a.tell(3.5);
        a.tell(4f);
        // give enough time to run
        Thread.sleep(500);
        context.shutdownGracefully().get(5000, TimeUnit.SECONDS);
    }

    @Test
    public void testMatchEquals() throws InterruptedException, ExecutionException, TimeoutException {
        Context context = Context.create();
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

    @Test
    public void testDelayed() {
        Context c = Context.create();
        AtomicInteger count = new AtomicInteger();
        TestScheduler ts = Scheduler.test();
        ActorRef<Object> a = c.matchAny(m -> count.incrementAndGet()).scheduler(ts).build();
        assertEquals(0, count.get());
        a.tell("hi");
        assertEquals(1, count.get());
        a.scheduler().schedule(() -> a.tell("boo"), 2, TimeUnit.SECONDS);
        assertEquals(1, count.get());
        ts.advance(1, TimeUnit.SECONDS);
        assertEquals(1, count.get());
        ts.advance(1, TimeUnit.SECONDS);
        assertEquals(2, count.get());
    }

    @Test
    public void testScheduledWithDelay() throws InterruptedException {
        for (Scheduler scheduler : new Scheduler[] { Scheduler.forkJoin(), Scheduler.computation(),
                Scheduler.immediate(), Scheduler.io() }) {
            Context c = Context.create();
            AtomicInteger n = new AtomicInteger();
            ActorRef<Object> a = c.matchAny(m -> n.incrementAndGet()).scheduler(scheduler).build();
            a.tell(1);
            a.scheduler().schedule(() -> a.tell(2), 10, TimeUnit.MILLISECONDS);
            Thread.sleep(100);
            assertEquals(2, n.get());
            c.shutdownGracefully();
        }
    }

    public static final class MyActor extends AbstractActor<Integer> {

        static volatile Integer last;

        @Override
        public void onMessage(Message<Integer> message) {
            last = message.content();
        }

    }

    public static final class MyActorBad extends AbstractActor<Integer> {

        public MyActorBad(String s) {

        }

        @Override
        public void onMessage(Message<Integer> message) {
        }

    }

}
