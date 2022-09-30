package com.github.davidmoten.reels.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OpenHashSetTest {

    @Test
    public void testToString() {
        OpenHashSet<String> set = new OpenHashSet<String>();
        set.add("hello");
        set.add("you");
        assertEquals("Set[you, hello]", set.toString());
    }

    @Test
    public void testAdd() {
        OpenHashSet<String> set = new OpenHashSet<String>();
        assertTrue(set.add("hello"));
        assertFalse(set.add("hello"));
        assertEquals(1, set.size());
        assertFalse(set.isEmpty());
    }

    @Test
    public void testAddManyThatOccupySameSlot() {
        OpenHashSet<MyObject> set = new OpenHashSet<>();
        for (int i = 0; i < 1050; i++) {
            set.add(new MyObject(i));
        }
        for (int i = 0; i < 1050; i++) {
            set.remove(new MyObject(i));
        }
        assertEquals(0, set.size());
    }

    private static final class MyObject {

        int a = 0;

        public MyObject(int a) {
            this.a = a;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MyObject other = (MyObject) obj;
            return a == other.a;
        }

    }

}
