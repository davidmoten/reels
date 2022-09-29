package com.github.davidmoten.reels.internal;

import java.util.concurrent.Executors;

import com.github.davidmoten.reels.Scheduler;

public final class Constants {

    private Constants() {
        // prevent instantiation
    }

    public static final Scheduler SINGLE = Scheduler.fromExecutor(Executors.newSingleThreadScheduledExecutor());

    public static final String DEAD_LETTER_ACTOR_NAME = "reels-dead-letter";

    public static final Object TERMINATED = new Object() {
        @Override
        public String toString() {
            return "TERMINATED";
        }
    };

    public static final String ROOT_ACTOR_NAME = "reels-root";
}
