package com.github.davidmoten.reels;

import static org.junit.Assert.assertTrue;

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

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.pattern.Patterns;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

@State(Scope.Benchmark)
public class BenchmarksAkka {

//    private static final Logger log = LoggerFactory.getLogger(BenchmarksAkka.class);

    private ActorSystem system;
    private akka.actor.ActorRef askActor;

    @Setup(Level.Invocation)
    public void setup() {
        system = ActorSystem.create();
        askActor = system.actorOf(Props.create(Test.class));
    }

    @TearDown(Level.Invocation)
    public void tearDown() throws InterruptedException, ExecutionException, TimeoutException {
        system.terminate();
        system.getWhenTerminated().toCompletableFuture().join();
    }

    public static final class Test extends akka.actor.AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder().matchAny(m -> sender().tell("boo", self())).build();
        }

    }

//    @Benchmark
//    @BenchmarkMode(Mode.Throughput)
//    public void actorCreateAndStop() throws InterruptedException {
//        system.actorOf(Props.create(Test.class)).tell(Boolean.TRUE, null);
//    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void ask(Blackhole bh) throws Exception {
        for (int i = 0; i < 10000; i++) {
            bh.consume(Await.result(Patterns.ask(askActor, "hi", 1000), Duration.create(1000, TimeUnit.MILLISECONDS)));
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void contendedConcurrencyDefaultDispatcher() throws InterruptedException {
        contendedConcurrency(Benchmarks.MESSAGES_PER_RUNNER);
    }

    private void contendedConcurrency(int messagesPerRunner) throws InterruptedException {
        int runners = 100;
        CountDownLatch latch = new CountDownLatch(1);
        ActorRef root = system.actorOf(Props.create(Manager.class, runners, messagesPerRunner, latch));
        root.tell(Start.VALUE, root);
        assertTrue(latch.await(60, TimeUnit.SECONDS));
    }

    private enum Start {
        VALUE;
    }

    static final class Manager extends akka.actor.AbstractActor {

        private final int runners;
        private final int messagesPerRunner;
        private final CountDownLatch latch;
        int count;

        Manager(int runners, int messagesPerRunner, CountDownLatch latch) {
            this.runners = runners;
            this.messagesPerRunner = messagesPerRunner;
            this.count = messagesPerRunner * runners;
            this.latch = latch;
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder() //
                    .match(Start.class, m -> {
                        for (int i = 0; i < runners; i++) {
                            akka.actor.ActorRef actor = context().actorOf(Props.create(Runner.class));
                            for (int j = 0; j < messagesPerRunner; j++) {
                                actor.tell(new int[] { i, j }, self());
                            }
                        }
                    }) //
                    .match(int[].class, m -> {
                        count--;
                        if (count == 0) {
                            latch.countDown();
                        }
                    }).build();
        }
    }

    static final class Runner extends akka.actor.AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder() //
                    .matchAny(m -> sender().tell(m, self())) //
                    .build();
        }
    }

    public static void main(String[] args) throws Exception {
        BenchmarksAkka b = new BenchmarksAkka();
        while (true) {
            long t = System.currentTimeMillis();
            b.setup();
            b.contendedConcurrency(10000);
            b.tearDown();
            System.out.println(System.currentTimeMillis() - t + "ms");
        }
    }

}
