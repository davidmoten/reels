package com.github.davidmoten.reels;

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
        system.getWhenTerminated().toCompletableFuture().join();
    }

    public static final class Test extends akka.actor.AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder().matchAny(m -> sender().tell("boo", self())).build();
        }

    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void actorCreateAndStop() throws InterruptedException {
        system.actorOf(Props.create(Test.class)).tell(Boolean.TRUE, null);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void ask(Blackhole bh) throws Exception {
        for (int i = 0; i < 10000; i++) {
            bh.consume(Await.result(Patterns.ask(askActor, "hi", 1000), Duration.create(1000, TimeUnit.MILLISECONDS)));
        }
    }

}
