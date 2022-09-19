package com.github.davidmoten.reels.internal;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;

public class ExceptionsTest {

    @Test
    public void isUtilityClass() {
        Asserts.assertIsUtilityClass(Exceptions.class);
    }
    
}
