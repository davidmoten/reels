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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@State(Scope.Benchmark)
public class Benchmarks {

    private static final Logger log = LoggerFactory.getLogger(Benchmarks.class);

    private Context context;
    private final Random random = new Random();

    @Setup(Level.Invocation)
    public void setup() {
        context = new Context((c, actor, error) -> {
            log.error(actor.name() + ":" + error.getMessage(), error);
        });
    }

    @TearDown(Level.Invocation)
    public void tearDown() {
        context.dispose();
        context = null;
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public String ask() throws InterruptedException, ExecutionException, TimeoutException {
        ActorRef<String> actor = context
                .<String>matchAll((c, msg) -> c.sender().ifPresent(sender -> sender.tell("boo"))) //
                .build();
        return actor.<String>ask("hi").get(1000, TimeUnit.MILLISECONDS);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void contendedConcurrencyForkJoin() throws InterruptedException {
        contendedConcurrency(Scheduler.forkJoin());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void contendedConcurrencyComputationSticky() throws InterruptedException {
        contendedConcurrency(Scheduler.computation());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void contendedConcurrencyImmediate() throws InterruptedException {
        contendedConcurrency(Scheduler.immediate());
    }
    
    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void groupRandomMessagesForkJoin() throws InterruptedException {
        groupRandomMessages(Scheduler.forkJoin());
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void groupRandomMessagesComputationSticky() throws InterruptedException {
        groupRandomMessages(Scheduler.computation());
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

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void actorCreateAndStop() throws InterruptedException {
        context //
                .matchAll((c, m) -> c.self().stop()) //
                .scheduler(Scheduler.immediate()) //
                .build() //
                .tell(Boolean.TRUE);
    }

    private void groupRandomMessages(Scheduler scheduler) throws InterruptedException {
        int numMessages = 100000;
        int numActors = 10;

        // this is how many messages are pinging around simultaneously at any one time
        int starters = Runtime.getRuntime().availableProcessors();
        CountDownLatch latch = new CountDownLatch(starters);
        for (int i = 0; i < numActors; i++) {
            context.<Integer>matchAll((c, msg) -> {
                if (msg == numMessages) {
                    latch.countDown();
                } else {
                    c.context() //
                            .lookupActor(Integer.toString(random.nextInt(numActors))) //
                            .ifPresent(x -> x.tell(msg + 1));
                }
            }).name(Integer.toString(i)).build();
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

    private void contendedConcurrency(Scheduler scheduler) throws InterruptedException {
        int runners = 100;
        int messagesPerRunner = 10000;
        CountDownLatch latch = new CountDownLatch(1);
        int[] count = new int[] { runners * messagesPerRunner };
        ActorRef<Object> root = context //
                .<Object, Start>match(Start.class, (c, msg) -> {
                    for (int i = 0; i < runners; i++) {
                        ActorRef<int[]> actor = c.context() //
                                .<int[]>matchAll((c2, msg2) -> {
                                    c2.sender().get().tell(msg2, c2.self());
                                }) //
                                .scheduler(scheduler) //
                                .build();
                        for (int j = 0; j < messagesPerRunner; j++) {
                            actor.tell(new int[] { i, j }, c.self());
                        }
                    }
                }) //
                .match(int[].class, (c, msg) -> {
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

}
