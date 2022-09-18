package com.github.davidmoten.reels.internal;

import java.util.concurrent.Executors;

import com.github.davidmoten.reels.Scheduler;

public final class Constants {

    private Constants() {
        // prevent instantiation
    }
    
    public static final Scheduler SINGLE = Scheduler.fromExecutor(Executors.newSingleThreadScheduledExecutor());
    
}
