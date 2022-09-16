package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class SchedulerHelper {
    
    private SchedulerHelper() {
        // prevent instantiation
    }
    
    public static ThreadFactory createThreadFactory(String prefix) {
        AtomicInteger count = new AtomicInteger();
        return r -> {
            String name = prefix + "-" + count.incrementAndGet();
            Thread t = new Thread(r, name);
            t.setPriority(Thread.NORM_PRIORITY);
            t.setDaemon(true);
            return t;
        };
    }

    
}
