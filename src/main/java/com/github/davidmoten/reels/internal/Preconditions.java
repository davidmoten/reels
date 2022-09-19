package com.github.davidmoten.reels.internal;

public final class Preconditions {

    private Preconditions() {
        // prevent instantiation
    }

    public static <T> T checkNotNull(T value) {
        if (value == null) {
            throw new IllegalArgumentException("cannot be null");
        } else {
            return value;
        }
    }

    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        } else {
            return value;
        }
    }
    
    public static <T> T checkParameterNotNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        } else {
            return value;
        }
    }

    public static void checkArgument(boolean b, String message) {
        if (b) {
            throw new IllegalArgumentException(message);
        }
    }

}
