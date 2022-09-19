package com.github.davidmoten.reels;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CreateExceptionTest {

    @Test
    public void testConstructors() {
        assertEquals("boo", new CreateException("boo").getMessage());
        IllegalArgumentException e = new IllegalArgumentException("thing");
        {
            CreateException ex = new CreateException("boo", e);
            assertEquals("boo", ex.getMessage());
            assertEquals(e, ex.getCause());
        }
        {
            CreateException ex = new CreateException(e);
            assertEquals("java.lang.IllegalArgumentException: thing", ex.getMessage());
            assertEquals(e, ex.getCause());
        }
    }

}
