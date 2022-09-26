package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.ActorRef;
import com.github.davidmoten.reels.Context;

public class MassiveTellToSingleActorReels {

    public static void main(String[] args) throws InterruptedException {
        for (int j = 0; j < 20; j++) {
            Context c = new Context();
            int n = 100000;
            int[] count = new int[1];
            CountDownLatch latch = new CountDownLatch(1);
            long t = System.currentTimeMillis();
            ActorRef<Integer> main = c.match(Integer.class, m -> {
                int x = m.content();
                if (x == -1) {
                    // start
                    for (int i = 0; i < n; i++) {
                        ActorRef<Integer> runner = m.context().match(Integer.class, m2 -> {
                            m2.<Integer>senderRaw().tell(m2.content());
                        }).build();
                        runner.tell(i, m.self());
                    }
                } else {
                    count[0]++;
                    if (count[0] == n) {
                        latch.countDown();
                    }
                }
            }).build();
            main.tell(-1);
            assertTrue(latch.await(60, TimeUnit.SECONDS));
            System.out.println((System.currentTimeMillis() - t) + "ms");
        }
    }

}
