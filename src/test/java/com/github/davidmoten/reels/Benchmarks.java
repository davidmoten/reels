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

@State(Scope.Benchmark)
public class Benchmarks {

    private Context context;

    @Setup(Level.Invocation)
    public void setup() {
        context = new Context();
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

    private enum Start {
        VALUE;
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
    public void groupRandomMessagesImmediate() throws InterruptedException {
        groupRandomMessages(Scheduler.immediate());
    }

    private void groupRandomMessages(Scheduler scheduler) throws InterruptedException {
        int numMessages = 10000;
        int numActors = 10;
        Random random = new Random();
        CountDownLatch latch = new CountDownLatch(1);
        for (int i = 0; i < numActors; i++) {
            context.<Integer>matchAll((c, msg) -> {
                if (msg == numMessages) {
                    latch.countDown();
                } else {
                    c.context() //
                            .lookupActor(Integer.toString(random.nextInt(numActors))) //
                            .get() //
                            .tell(msg + 1);
                }
            }).name(Integer.toString(i)).build();
        }
        context.<Integer>lookupActor("0").get().tell(0);
        assertTrue(latch.await(60, TimeUnit.SECONDS));
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
