package com.github.davidmoten.reels;

import akka.actor.ActorSystem;
import akka.actor.Props;

public class MemoryUsageAkkaMain {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create();
        for (int i = 0; i < 1_000_000; i++) {
            system.actorOf(Props.create(Test.class));
        }
        Thread.sleep(100000000L);
    }

    public static final class Test extends akka.actor.AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder().matchAny(m -> {
            }).build();
        }

    }

}
