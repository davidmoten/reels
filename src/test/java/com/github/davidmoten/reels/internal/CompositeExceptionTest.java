/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package com.github.davidmoten.reels.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class CompositeExceptionTest  {

    private final Throwable ex1 = new Throwable("Ex1");
    private final Throwable ex2 = new Throwable("Ex2", ex1);
    private final Throwable ex3 = new Throwable("Ex3", ex2);
    
    private final PrintStream err = createErr();

    private CompositeException getNewCompositeExceptionWithEx123() {
        List<Throwable> throwables = new ArrayList<>();
        throwables.add(ex1);
        throwables.add(ex2);
        throwables.add(ex3);
        return new CompositeException(throwables);
    }

    private static PrintStream createErr() {
        try {
            return new PrintStream(new File("target/err.txt"));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void multipleWithSameCause() {
        Throwable rootCause = new Throwable("RootCause");
        Throwable e1 = new Throwable("1", rootCause);
        Throwable e2 = new Throwable("2", rootCause);
        Throwable e3 = new Throwable("3", rootCause);
        CompositeException ce = new CompositeException(e1, e2, e3);

        err.println("----------------------------- print composite stacktrace");
        ce.printStackTrace(err);
        assertEquals(3, ce.getExceptions().size());

        assertNoCircularReferences(ce);
        assertNotNull(getRootCause(ce));
        err.println("----------------------------- print cause stacktrace");
        ce.getCause().printStackTrace(err);
    }
    
    @Test
    public void testPrintStackTrace() {
        Throwable rootCause = new Throwable("RootCause");
        Throwable e1 = new Throwable("1", rootCause);
        CompositeException ce = new CompositeException(e1);
        PrintStream stderr = System.err;
        System.setErr(err);
        ce.printStackTrace();
        System.setErr(stderr);
    }

    @Test
    public void emptyErrors() {
        try {
            new CompositeException();
            fail("CompositeException should fail if errors is empty");
        } catch (IllegalArgumentException e) {
            assertEquals("errors is empty", e.getMessage());
        }
        try {
            new CompositeException(new ArrayList<>());
            fail("CompositeException should fail if errors is empty");
        } catch (IllegalArgumentException e) {
            assertEquals("errors is empty", e.getMessage());
        }
    }

    @Test
    public void compositeExceptionFromParentThenChild() {
        CompositeException cex = new CompositeException(ex1, ex2);

        err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace(err);
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace(err);
    }

    @Test
    public void compositeExceptionFromChildThenParent() {
        CompositeException cex = new CompositeException(ex2, ex1);

        err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace(err);
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace(err);
    }

    @Test
    public void compositeExceptionFromChildAndComposite() {
        CompositeException cex = new CompositeException(ex1, getNewCompositeExceptionWithEx123());

        err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace(err);
        assertEquals(3, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace(err);
    }

    @Test
    public void compositeExceptionFromCompositeAndChild() {
        CompositeException cex = new CompositeException(getNewCompositeExceptionWithEx123(), ex1);

        err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace(err);
        assertEquals(3, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace(err);
    }

    @Test
    public void compositeExceptionFromTwoDuplicateComposites() {
        List<Throwable> exs = new ArrayList<>();
        exs.add(getNewCompositeExceptionWithEx123());
        exs.add(getNewCompositeExceptionWithEx123());
        CompositeException cex = new CompositeException(exs);

        err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace(err);
        assertEquals(3, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace(err);
    }

    /*
     * This hijacks the Throwable.printStackTrace(err) output and puts it in a string, where we can look for
     * "CIRCULAR REFERENCE" (a String added by Throwable.printEnclosedStackTrace)
     */
    private static void assertNoCircularReferences(Throwable ex) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);
        ex.printStackTrace(printStream);
        assertFalse(baos.toString().contains("CIRCULAR REFERENCE"));
    }

    private static Throwable getRootCause(Throwable ex) {
        Throwable root = ex.getCause();
        if (root == null) {
            return null;
        } else {
            while (true) {
                if (root.getCause() == null) {
                    return root;
                } else {
                    root = root.getCause();
                }
            }
        }
    }

    @Test
    public void nullCollection() {
        CompositeException composite = new CompositeException((List<Throwable>)null);
        composite.getCause();
        composite.printStackTrace(err);
    }

    @Test
    public void nullElement() {
        CompositeException composite = new CompositeException(Collections.singletonList((Throwable) null));
        composite.getCause();
        composite.printStackTrace(err);
    }

    @Test
    public void compositeExceptionWithUnsupportedInitCause() {
        Throwable t = new Throwable() {

            private static final long serialVersionUID = -3282577447436848385L;

            @Override
            public synchronized Throwable initCause(Throwable cause) {
                throw new UnsupportedOperationException();
            }
        };
        CompositeException cex = new CompositeException(t, ex1);

        err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace(err);
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace(err);
    }

    @Test
    public void compositeExceptionWithNullInitCause() {
        Throwable t = new Throwable("ThrowableWithNullInitCause") {

            private static final long serialVersionUID = -7984762607894527888L;

            @Override
            public synchronized Throwable initCause(Throwable cause) {
                return null;
            }
        };
        CompositeException cex = new CompositeException(t, ex1);

        err.println("----------------------------- print composite stacktrace");
        cex.printStackTrace(err);
        assertEquals(2, cex.getExceptions().size());

        assertNoCircularReferences(cex);
        assertNotNull(getRootCause(cex));

        err.println("----------------------------- print cause stacktrace");
        cex.getCause().printStackTrace(err);
    }

    @Test
    public void messageCollection() {
        CompositeException compositeException = new CompositeException(ex1, ex3);
        assertEquals("2 exceptions occurred. ", compositeException.getMessage());
    }

    @Test
    public void messageVarargs() {
        CompositeException compositeException = new CompositeException(ex1, ex2, ex3);
        assertEquals("3 exceptions occurred. ", compositeException.getMessage());
    }

    @Test
    public void constructorWithNull() {
        assertTrue(new CompositeException((Throwable[])null).getExceptions().get(0) instanceof NullPointerException);

        assertTrue(new CompositeException((Iterable<Throwable>)null).getExceptions().get(0) instanceof NullPointerException);

        assertTrue(new CompositeException(null, new TestException()).getExceptions().get(0) instanceof NullPointerException);
    }

    @Test
    public void printStackTrace() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        new CompositeException(new TestException()).printStackTrace(pw);

        assertTrue(sw.toString().contains("TestException"));
    }

    @Test
    public void badException() {
        Throwable e = new BadException();
        assertSame(e, new CompositeException(e).getCause().getCause());
        assertSame(e, new CompositeException(new RuntimeException(e)).getCause().getCause().getCause());
    }

    @Test
    public void exceptionOverview() {
        CompositeException composite = new CompositeException(
                new TestException("ex1"),
                new TestException("ex2"),
                new TestException("ex3", new TestException("ex4"))
        );

        String overview = composite.getCause().getMessage();

        assertTrue(overview, overview.contains("Multiple exceptions (3)"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.TestException: ex1"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.TestException: ex2"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.TestException: ex3"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.TestException: ex4"));
        assertTrue(overview, overview.contains("at com.github.davidmoten.reels.internal.CompositeExceptionTest.exceptionOverview"));
    }

    @Test
    public void causeWithExceptionWithoutStacktrace() {
        CompositeException composite = new CompositeException(
                new TestException("ex1"),
                new CompositeException.ExceptionOverview("example")
        );

        String overview = composite.getCause().getMessage();
        System.out.println(overview);

        assertTrue(overview, overview.contains("Multiple exceptions (2)"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.TestException: ex1"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.CompositeException.ExceptionOverview: example"));

        assertEquals(overview, 2, overview.split("at\\s").length);
    }

    @Test
    public void reoccurringException() {
        TestException ex0 = new TestException("ex0");
        TestException ex1 = new TestException("ex1", ex0);
        CompositeException composite = new CompositeException(
                ex1,
                new TestException("ex2", ex1)
        );

        String overview = composite.getCause().getMessage();
        err.println(overview);

        assertTrue(overview, overview.contains("Multiple exceptions (2)"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.TestException: ex0"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.TestException: ex1"));
        assertTrue(overview, overview.contains("com.github.davidmoten.reels.internal.TestException: ex2"));
        assertTrue(overview, overview.contains("(cause not expanded again) com.github.davidmoten.reels.internal.TestException: ex0"));
        assertEquals(overview, 5, overview.split("at\\s").length);
    }

    @Test
    public void nestedMultilineMessage() {
        TestException ex1 = new TestException("ex1");
        TestException ex2 = new TestException("ex2");
        CompositeException composite1 = new CompositeException(
                ex1,
                ex2
        );
        TestException ex3 = new TestException("ex3");
        TestException ex4 = new TestException("ex4", composite1);

        CompositeException composite2 = new CompositeException(
                ex3,
                ex4
        );

        String overview = composite2.getCause().getMessage();
        err.println(overview);

        assertTrue(overview, overview.contains("        Multiple exceptions (2)"));
        assertTrue(overview, overview.contains("        |-- com.github.davidmoten.reels.internal.TestException: ex1"));
        assertTrue(overview, overview.contains("        |-- com.github.davidmoten.reels.internal.TestException: ex2"));
    }

    @Test
    public void singleExceptionIsTheCause() {
        TestException ex = new TestException("ex1");
        CompositeException composite = new CompositeException(ex);

        assertSame(composite.getCause(), ex);
        assertEquals(1, composite.size());
    }
}

final class TestException extends Exception {
    private static final long serialVersionUID = -5597058852216656719L;
    TestException(String message) {
        super(message);
    }
    TestException(String message, Throwable e) {
        super(message, e);
    }
    TestException() {
        super();
    }
}

final class BadException extends Throwable {
    private static final long serialVersionUID = 8999507293896399171L;

    @Override
    public synchronized Throwable getCause() {
        return this;
    }
}