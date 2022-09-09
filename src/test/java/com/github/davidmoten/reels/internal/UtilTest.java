package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class UtilTest {

    @Test
    public void testRethrowChecked() {
        try {
            Util.rethrow(new IOException("boo"));
        } catch (RuntimeException e) {
            assertEquals("boo", e.getCause().getMessage());
        }
    }

    @Test
    public void testRethrowError() {
        try {
            Util.rethrow(new OutOfMemoryError());
        } catch (OutOfMemoryError e) {
            // good
        } catch (Throwable e) {
            Assert.fail();
        }
    }

}
