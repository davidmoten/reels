package com.github.davidmoten.reels;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadBalancerExampleMain {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerExampleMain.class);

    public static void main(String[] args) throws InterruptedException {
        Context context = Context.create();
        AtomicInteger count = new AtomicInteger();
        Function<ActorRef<Integer>, ActorRef<Integer>> workerFactory = parent -> context
                .<Integer>matchAny(m -> log.info(m.self().name() + " received " + m.content())) //
                .name("worker" + Integer.toString(count.incrementAndGet())) //
                .parent(parent) //
                .build();
        ActorRef<Integer> balancer = context
                .actorFactory(() -> new Balancer<Integer>(workerFactory, 5)).build();
        for (int i = 0; i < 20; i++) {
            balancer.tell(i);
        }
        Thread.sleep(2000);
        context.shutdownGracefully().join();
    }

    public static class Balancer<T> extends AbstractActor<T> {

        /**
         *  Given the Balancer ActorRef, creates a worker. The worker should set the parent to the Balancer. If you don't then stopping the Balancer will not stop the workers.
         */
        private final Function<ActorRef<T>, ActorRef<T>> workerFactory;
        private final int size;

        private List<? extends ActorRef<T>> actors;
        private int index;

        public Balancer(Function<ActorRef<T>, ActorRef<T>> actorFactory, int size) {
            this.workerFactory = actorFactory;
            this.size = size;
        }
        
        @Override
        public void preStart(ActorRef<T> self) {
            actors = IntStream //
                    .range(0,  size) //
                    .mapToObj(i -> workerFactory.apply(self)) //
                    .collect(Collectors.toList());
        }

        @Override
        public void onMessage(Message<T> message) {
            actors.get(index).tell(message.content(), message.sender());
            index = (index + 1) % actors.size();
        }

    }
}
