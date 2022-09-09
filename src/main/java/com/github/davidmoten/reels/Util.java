package com.github.davidmoten.reels;

public final class Util {

    static int systemPropertyInt(String name, int defaultValue) {
        return Integer.parseInt(System.getProperty(name, defaultValue + ""));
    }

}
