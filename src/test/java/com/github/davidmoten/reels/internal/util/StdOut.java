package com.github.davidmoten.reels.internal.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public final class StdOut implements AutoCloseable {
    
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(bytes);
    private PrintStream previous;

    public static StdOut create() {
        StdOut s = new StdOut();
        s.start();
        return s;
    }

    
    private StdOut() {
        
    }
    
    private void start() {
        previous = System.out;
        System.setOut(out);
    }
    
    public String text() {
        out.flush();
        try {
            return bytes.toString("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void close() throws Exception {
        out.close();
        System.setOut(previous);
    }
}
