package com.github.davidmoten.reels.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FastRandomIntTest {

    @Test
    public void test() {
        FastRandomInt r = new FastRandomInt();
        int n = 1000000;
        int total = 0;
        for (int i = 0; i < n; i++) {
            total += r.nextInt(101);
        }
        double average = ((double) total) / n;
        assertEquals(50, average, 0.1);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testBadBound() {
        FastRandomInt r = new FastRandomInt();
        r.nextInt(-1);
    }
    
    @Test
    public void testPowerOfTwoBounds() {
        FastRandomInt r = new FastRandomInt();
        assertTrue(r.nextInt(8) < 8);
    }

}
