package com.github.davidmoten.reels.internal.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class UtilTest {

    @Test
    public void testPrimitiveTypesMatch() {
        assertTrue(Util.primitiveTypesMatch(byte.class, Byte.class));
        assertTrue(Util.primitiveTypesMatch(short.class, Short.class));
        assertTrue(Util.primitiveTypesMatch(int.class, Integer.class));
        assertTrue(Util.primitiveTypesMatch(long.class, Long.class));
        assertTrue(Util.primitiveTypesMatch(float.class, Float.class));
        assertTrue(Util.primitiveTypesMatch(double.class, Double.class));
        assertTrue(Util.primitiveTypesMatch(char.class, Character.class));
        assertTrue(Util.primitiveTypesMatch(boolean.class, Boolean.class));
    }

    @Test
    public void testPrimitiveTypesDoNotMatch() {
        assertFalse(Util.primitiveTypesMatch(byte.class, Short.class));
        assertFalse(Util.primitiveTypesMatch(short.class, Integer.class));
        assertFalse(Util.primitiveTypesMatch(int.class, Long.class));
        assertFalse(Util.primitiveTypesMatch(long.class, Float.class));
        assertFalse(Util.primitiveTypesMatch(float.class, Double.class));
        assertFalse(Util.primitiveTypesMatch(double.class, Character.class));
        assertFalse(Util.primitiveTypesMatch(char.class, Byte.class));
        assertFalse(Util.primitiveTypesMatch(boolean.class, Byte.class));
    }

}
