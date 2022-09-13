package com.github.davidmoten.reels.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class Util {

    private Util() {
        // prevent instantiation
    }

    public static int systemPropertyInt(String name, int defaultValue) {
        return Integer.parseInt(System.getProperty(name, defaultValue + ""));
    }

    public static void rethrow(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else if (e instanceof Error) {
            throw (Error) e;
        } else {
            throw new RuntimeException(e);
        }
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
