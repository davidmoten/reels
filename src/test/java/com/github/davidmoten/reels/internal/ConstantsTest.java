package com.github.davidmoten.reels.internal;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;

public class ConstantsTest {

    @Test
    public void isUtilityClass() {
        Asserts.assertIsUtilityClass(Constants.class);
    }

}
