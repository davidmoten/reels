package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;

public class PreconditionsTest {

    @Test
    public void isUtilityClass() {
        Asserts.assertIsUtilityClass(Preconditions.class);
    }

    @Test
    public void testArgumentNonNullWhenNull() {
        try {
            Preconditions.checkArgumentNonNull(null, "hello");
        } catch (IllegalArgumentException e) {
            assertEquals("hello cannot be null", e.getMessage());
        }
    }

    @Test
    public void testArgumentNonNullWhenNonNull() {
        assertEquals(Boolean.TRUE, Preconditions.checkArgumentNonNull(Boolean.TRUE, "hello"));
    }

    @Test
    public void testCheckArgumentWhenFalse() {
        try {
            Preconditions.checkArgument(false, "hello");
        } catch (IllegalArgumentException e) {
            assertEquals("hello", e.getMessage());
        }
    }

    @Test
    public void testCheckArgumentWhenTrue() {
        Preconditions.checkArgument(true, "hello");
    }
}
