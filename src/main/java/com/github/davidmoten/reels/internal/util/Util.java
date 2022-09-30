package com.github.davidmoten.reels.internal.util;

public final class Util {

    private Util() {
        // prevent instantiation
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

}
