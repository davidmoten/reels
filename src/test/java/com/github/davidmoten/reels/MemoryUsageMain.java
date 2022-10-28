package com.github.davidmoten.reels;

public class MemoryUsageMain {

    public static void main(String[] args) throws InterruptedException {
        Context context = Context.create();
        int n = 1_000_000;
        for (int i = 0; i < n; i++) {
            context.matchAny(m -> {}).build();
        }
        Thread.sleep(10000000L);
    }

}
