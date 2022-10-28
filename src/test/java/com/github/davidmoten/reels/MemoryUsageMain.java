package com.github.davidmoten.reels;

public class MemoryUsageMain {

    public static void main(String[] args) throws InterruptedException {
        boolean useMatcher = false;
        Context context = Context.create();
        int n = 1_000_000;
        for (int i = 0; i < n; i++) {
            if (useMatcher) {
                context.matchAny(m -> {
                }).build();
            } else {
                context.actorClass(Thing.class).build();
            }
        }
        Thread.sleep(10000000L);
    }

    static final class Thing extends AbstractActor<Object> {

        @Override
        public void onMessage(Message<Object> message) {

        }
    }

}
