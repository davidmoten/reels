package com.github.davidmoten.reels.internal.util;

/**
 * A cut-down version of java.util.Random that does not use atomics or volatiles
 * for better single-threaded performance.
 */
public final class FastRandomInt {

    /**
     * The internal state associated with this pseudorandom number generator. (The
     * specs for the methods in this class describe the ongoing computation of this
     * value.)
     */
    private long seed;

    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;
    private static final long mask = (1L << 48) - 1;

    // IllegalArgumentException messages
    static final String BadBound = "bound must be positive";
    static final String BadRange = "bound must be greater than origin";
    static final String BadSize = "size must be non-negative";

    /**
     * Creates a new random number generator. This constructor sets the seed of the
     * random number generator to a value very likely to be distinct from any other
     * invocation of this constructor.
     */
    public FastRandomInt() {
        seed = initialScramble(8682522807148012L ^ System.nanoTime());
    }

    private static long initialScramble(long seed) {
        return (seed ^ multiplier) & mask;
    }

    /**
     * Generates the next pseudorandom number. Subclasses should override this, as
     * this is used by all other methods.
     *
     * <p>
     * The general contract of {@code next} is that it returns an {@code int} value
     * and if the argument {@code bits} is between {@code 1} and {@code 32}
     * (inclusive), then that many low-order bits of the returned value will be
     * (approximately) independently chosen bit values, each of which is
     * (approximately) equally likely to be {@code 0} or {@code 1}. The method
     * {@code next} is implemented by class {@code Random} by atomically updating
     * the seed to
     * 
     * <pre>{@code
     * (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1)
     * }</pre>
     * 
     * and returning
     * 
     * <pre>{@code
     * (int) (seed >>> (48 - bits))
     * }.</pre>
     *
     * This is a linear congruential pseudorandom number generator, as defined by D.
     * H. Lehmer and described by Donald E. Knuth in <i>The Art of Computer
     * Programming,</i> Volume 3: <i>Seminumerical Algorithms</i>, section 3.2.1.
     *
     * @param bits random bits
     * @return the next pseudorandom value from this random number generator's
     *         sequence
     * @since 1.1
     */
    private int next(int bits) {
        seed = (seed * multiplier + addend) & mask;
        return (int) (seed >>> (48 - bits));
    }

    /**
     * Returns the next pseudorandom, uniformly distributed {@code int} value from
     * this random number generator's sequence. The general contract of
     * {@code nextInt} is that one {@code int} value is pseudorandomly generated and
     * returned. All 2<sup>32</sup> possible {@code int} values are produced with
     * (approximately) equal probability.
     *
     * <p>
     * The method {@code nextInt} is implemented by class {@code Random} as if by:
     * 
     * <pre> {@code
     * public int nextInt() {
     *     return next(32);
     * }
     * }</pre>
     *
     * @return the next pseudorandom, uniformly distributed {@code int} value from
     *         this random number generator's sequence
     */
    public int nextInt() {
        return next(32);
    }

    /**
     * Returns a pseudorandom, uniformly distributed {@code int} value between 0
     * (inclusive) and the specified value (exclusive), drawn from this random
     * number generator's sequence. The general contract of {@code nextInt} is that
     * one {@code int} value in the specified range is pseudorandomly generated and
     * returned. All {@code bound} possible {@code int} values are produced with
     * (approximately) equal probability. The method {@code nextInt(int bound)} is
     * implemented by class {@code Random} as if by:
     * 
     * <pre> {@code
     * public int nextInt(int bound) {
     *     if (bound <= 0)
     *         throw new IllegalArgumentException("bound must be positive");
     *
     *     if ((bound & -bound) == bound) // i.e., bound is a power of 2
     *         return (int) ((bound * (long) next(31)) >> 31);
     *
     *     int bits, val;
     *     do {
     *         bits = next(31);
     *         val = bits % bound;
     *     } while (bits - val + (bound - 1) < 0);
     *     return val;
     * }
     * }</pre>
     *
     * <p>
     * The hedge "approximately" is used in the foregoing description only because
     * the next method is only approximately an unbiased source of independently
     * chosen bits. If it were a perfect source of randomly chosen bits, then the
     * algorithm shown would choose {@code int} values from the stated range with
     * perfect uniformity.
     * <p>
     * The algorithm is slightly tricky. It rejects values that would result in an
     * uneven distribution (due to the fact that 2^31 is not divisible by n). The
     * probability of a value being rejected depends on n. The worst case is
     * n=2^30+1, for which the probability of a reject is 1/2, and the expected
     * number of iterations before the loop terminates is 2.
     * <p>
     * The algorithm treats the case where n is a power of two specially: it returns
     * the correct number of high-order bits from the underlying pseudo-random
     * number generator. In the absence of special treatment, the correct number of
     * <i>low-order</i> bits would be returned. Linear congruential pseudo-random
     * number generators such as the one implemented by this class are known to have
     * short periods in the sequence of values of their low-order bits. Thus, this
     * special case greatly increases the length of the sequence of values returned
     * by successive calls to this method if n is a small power of two.
     *
     * @param bound the upper bound (exclusive). Must be positive.
     * @return the next pseudorandom, uniformly distributed {@code int} value
     *         between zero (inclusive) and {@code bound} (exclusive) from this
     *         random number generator's sequence
     * @throws IllegalArgumentException if bound is not positive
     * @since 1.2
     */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BadBound);
        }

        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0) // i.e., bound is a power of 2
            r = (int) ((bound * (long) r) >> 31);
        else {
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31)); // NOPMD
        }
        return r;
    }
}
