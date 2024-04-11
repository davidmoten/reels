package com.github.davidmoten.reels;

public final class PoisonPill {

    private static final PoisonPill INSTANCE = new PoisonPill();
    
    private PoisonPill() {
        // prevent instantiation
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T instance() {
        return (T) INSTANCE;
    }
    
    public String toString() {
        return "PoisonPill";
    }
}