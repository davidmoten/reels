package com.github.davidmoten.reels;

public final class Util {

    static int systemPropertyInt(String name, int defaultValue) {
        return Integer.parseInt(System.getProperty(name, defaultValue + ""));
    }

    public static void rethrow(Throwable e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        } else if (e instanceof Error) {
            throw (Error) e;
        } else {
            throw new RuntimeException(e);
        }
    }

}
