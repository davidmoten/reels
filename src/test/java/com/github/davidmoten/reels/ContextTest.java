package com.github.davidmoten.reels;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class ContextTest {

    @Test(expected = CreateException.class)
    public void testConstruct1()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        constructThrowing(new InstantiationException());
    }
    
    @Test(expected = CreateException.class)
    public void testConstruct2()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        constructThrowing(new IllegalAccessException());
    }
    
    @Test(expected = CreateException.class)
    public void testConstruct3()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        constructThrowing(new IllegalArgumentException());
    }

    @Test(expected = CreateException.class)
    public void testConstruct4()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
         constructThrowing(new InvocationTargetException(new RuntimeException()));
    }
    
    private void constructThrowing(Exception ex) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> c;
        try {
            c = Throws.class.getConstructor();
        } catch (NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(e);
        }
        Throws.exception = ex;
        Context.construct(c);
    }
    
    public static final class Throws {
        
        static Exception exception;
        
        public Throws() throws Exception {
            throw exception;
        }
    }
}
