package com.github.davidmoten.reels;

import static org.junit.Assert.assertTrue;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@State(Scope.Benchmark)
public class Benchmarks {

    static final int MESSAGES_PER_RUNNER = Integer.getInteger("messages", 10000);

    private static final Logger log = LoggerFactory.getLogger(Benchmarks.class);

    private final Random random = new Random();
    private Context context;
    private ActorRef<String> askActor;

    @Setup(Level.Invocation)
    public void setup() {
        context = Context //
                .builder() //
                .supervisor((c, actor, error) -> {
                    log.error(actor.name() + ":" + error.getMessage(), error);
                }) //
                .deadLetterActorFactory(ActorDoNothing::create) //
                .build();
        askActor = context.<String>matchAny(m -> m.sender().tell("boo")) //
                .build();
    }

    @TearDown(Level.Invocation)
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        context.shutdownGracefully().get(5, TimeUnit.SECONDS);
        context = null;
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void sequential() throws InterruptedException, ExecutionException, TimeoutException {
        int max = 1000000;
        CountDownLatch latch = new CountDownLatch(1);
        ActorRef<Integer> a = createSequentialActor(context, latch, -1, max);
        a.tell(0);
        assertTrue(latch.await(60, TimeUnit.SECONDS));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void actorCreateAndStop() throws InterruptedException {
        context //
                .matchAny(m -> m.self().stop()) //
                .scheduler(Scheduler.immediate()) //
                .build() //
                .tell(Boolean.TRUE);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void ask(Blackhole bh) throws InterruptedException, ExecutionException, TimeoutException {
        for (int i = 0; i < 10000; i++) {
            bh.consume(askActor.<String>ask("hi").get(1000, TimeUnit.MILLISECONDS));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void contendedConcurrencyForkJoin() throws InterruptedException {
        contendedConcurrency(Scheduler.forkJoin(), MESSAGES_PER_RUNNER);
    }

//    @Benchmark
//    @BenchmarkMode(Mode.AverageTime)
//    public void contendedConcurrencyForkJoinLong() throws InterruptedException {
//        contendedConcurrency(Scheduler.forkJoin(), 100000);
//   ) }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void contendedConcurrencyComputationSticky() throws InterruptedException {
        contendedConcurrency(Scheduler.computationSticky(), MESSAGES_PER_RUNNER);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void contendedConcurrencyImmediate() throws InterruptedException {
        contendedConcurrency(Scheduler.immediate(), MESSAGES_PER_RUNNER);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void groupRandomMessagesForkJoin() throws InterruptedException {
        groupRandomMessages(Scheduler.forkJoin());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void groupRandomMessagesComputationSticky() throws InterruptedException {
        groupRandomMessages(Scheduler.computationSticky());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void groupRandomMessagesIo() throws InterruptedException {
        groupRandomMessages(Scheduler.io());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void groupRandomMessagesImmediate() throws InterruptedException {
        groupRandomMessages(Scheduler.immediate());
    }

    private static ActorRef<Integer> createSequentialActor(Context c, CountDownLatch latch, int finished, int max) {
        return c.match(Integer.class, m -> {
            int x = m.content();
            ActorRef<Object> sender = m.sender();
            if (sender == ActorRef.none() && x == finished) {
                latch.countDown();
            } else if (x == max) {
                sender.tell(finished);
            } else {
                ActorRef<Integer> next = createSequentialActor(c, latch, finished, max);
                next.tell(x + 1, m.self());
            }
        }) //
                .scheduler(Scheduler.defaultScheduler()) //
                .build();
    }

    private void groupRandomMessages(Scheduler scheduler) throws InterruptedException {
        int numMessages = 100000;
        int numActors = 10;

        // this is how many messages are pinging around simultaneously at any one time
        int starters = Runtime.getRuntime().availableProcessors();
        CountDownLatch latch = new CountDownLatch(starters);
        for (int i = 0; i < numActors; i++) {
            context.<Integer>matchAny(m -> {
                if (m.content() == numMessages) {
                    latch.countDown();
                } else {
                    m.context() //
                            .lookupActor(Integer.toString(random.nextInt(numActors))) //
                            .ifPresent(x -> x.tell(m.content() + 1));
                }
            }) //
                    .scheduler(scheduler) //
                    .name(Integer.toString(i)) //
                    .build();
        }
        ActorRef<Integer> a = context.<Integer>lookupActor("0").get();
        for (int i = 0; i < starters; i++) {
            a.tell(0);
        }
        assertTrue(latch.await(60, TimeUnit.SECONDS));
    }

    private enum Start {
        VALUE;
    }

    private void contendedConcurrency(Scheduler scheduler, int messagesPerRunner) throws InterruptedException {
        int runners = 100;
        CountDownLatch latch = new CountDownLatch(1);
        int[] count = new int[] { runners * messagesPerRunner };
        ActorRef<Object> root = context //
                .<Object, Start>match(Start.class, m -> {
                    for (int i = 0; i < runners; i++) {
                        ActorRef<int[]> actor = m.context() //
                                .<int[]>matchAny(m2 -> m2.sender().tell(m2.content(), m2.self())) //
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
        root.tell(Start.VALUE);
        assertTrue(latch.await(60, TimeUnit.SECONDS));
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
//        Context context = new Context((c, actor, error) -> {
//            log.error(actor.name() + ":" + error.getMessage(), error);
//        });
////        ActorRef<String> askActor = context
//                .<String>matchAll((c, msg) -> c.sender().ifPresent(sender -> sender.tell("boo"))) //
//                .build();
        Benchmarks b = new Benchmarks();
        while (true) {
            b.setup();
            for (;;) {
                long t = System.currentTimeMillis();
                b.sequential();
                System.out.println((System.currentTimeMillis() - t) + "ms");
            }

//            b.tearDown();
        }
    }

}
