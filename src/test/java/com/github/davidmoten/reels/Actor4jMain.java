package com.github.davidmoten.reels;

import java.util.UUID;

import io.actor4j.core.ActorSystem;
import io.actor4j.core.messages.ActorMessage;

public class Actor4jMain {

    public static void main(String[] args) throws InterruptedException {
        ActorSystem s = new ActorSystem();
        UUID id = s.addActor(() -> new MyActor());
        s.send(new ActorMessage<>(1, 100, id, id));
        Thread.sleep(1000);
        s.shutdown(true);
    }

    public static final class MyActor extends io.actor4j.core.actors.Actor {

        @Override
        public void receive(ActorMessage<?> message) {
            System.out.println("received " + message.value);
        }

    }
}
