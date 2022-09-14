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

    /**
     * Find the next larger positive power of two value up from the given value. If value is a power of two then
     * this value will be returned.
     *
     * @param value from which next positive power of two will be found.
     * @return the next positive power of 2 or this value if it is a power of 2.
     */
    public static int roundToPowerOfTwo(final int value) {
        return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
    }

    /**
     * Is this value a power of two.
     *
     * @param value to be tested to see if it is a power of two.
     * @return true if the value is a power of 2 otherwise false.
     */
    public static boolean isPowerOfTwo(final int value) {
        return (value & (value - 1)) == 0;
    }

}
