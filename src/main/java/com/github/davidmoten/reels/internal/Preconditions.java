package com.github.davidmoten.reels.internal;

public final class Preconditions {

    private Preconditions() {
        // prevent instantiation
    }

    public static <T> T checkArgumentNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        } else {
            return value;
        }
    }

    public static void checkArgument(boolean b, String message) {
        if (!b) {
            throw new IllegalArgumentException(message);
        }
    }

}
