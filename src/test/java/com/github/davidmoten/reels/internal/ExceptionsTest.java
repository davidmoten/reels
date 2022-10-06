package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;

import junit.framework.Assert;

public class ExceptionsTest {

    @Test
    public void isUtilityClass() {
        Asserts.assertIsUtilityClass(Exceptions.class);
    }

    @Test
    public void throwIfVirtualMachineError() {
        Throwable t = new VirtualMachineError() {
        };
        try {
            Exceptions.throwIfFatal(t);
            Assert.fail();
        } catch (Throwable e) {
            assertTrue(t == e);
        }
    }
    
    //ThreadDeath
    //LinkageError

    @Test
    public void throwIfThreadDeath() {
        Throwable t = new ThreadDeath();
        try {
            Exceptions.throwIfFatal(t);
            Assert.fail();
        } catch (Throwable e) {
            assertTrue(t == e);
        }
    }
    
    @Test
    public void throwIfLinkageError() {
        Throwable t = new LinkageError();
        try {
            Exceptions.throwIfFatal(t);
            Assert.fail();
        } catch (Throwable e) {
            assertTrue(t == e);
        }
    }
}
