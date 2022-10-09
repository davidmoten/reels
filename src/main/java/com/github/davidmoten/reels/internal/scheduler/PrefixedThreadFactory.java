package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class PrefixedThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger count = new AtomicInteger();

    public PrefixedThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        String name = prefix + "-" + count.incrementAndGet();
        Thread t = new Thread(r, name);
        t.setPriority(Thread.NORM_PRIORITY);
        t.setDaemon(true);
        return t;
    }
}
