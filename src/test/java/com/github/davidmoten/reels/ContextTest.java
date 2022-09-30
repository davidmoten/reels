package com.github.davidmoten.reels;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;
import org.mockito.Mockito;

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

    private void constructThrowing(Throwable t) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> c = Mockito.mock(Constructor.class);
        Mockito.when(c.newInstance()).thenThrow(t);
        Context.construct(c);
    }
    
    @Test(expected = CreateException.class)
    public void testConstruct4()
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Constructor<?> c = Mockito.mock(Constructor.class);
        Mockito.when(c.newInstance()).thenThrow(InvocationTargetException.class);
        Context.construct(c);
    }

}
