package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;

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

}
