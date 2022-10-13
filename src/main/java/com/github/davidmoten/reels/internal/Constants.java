package com.github.davidmoten.reels.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.github.davidmoten.reels.Scheduler;
import com.github.davidmoten.reels.internal.scheduler.SchedulerHelper;

public final class Constants {

    private Constants() {
        // prevent instantiation
    }

    public static final Scheduler SINGLE = Scheduler.fromExecutor(Executors.newSingleThreadScheduledExecutor(SchedulerHelper.createThreadFactory("ReelsSingle")), false);
    
    public static final ThreadFactory NEW_SINGLE_THREAD_FACTORY = SchedulerHelper.createThreadFactory("ReelsNewSingle");

    public static final String DEAD_LETTER_ACTOR_NAME = "reels-dead-letter";

    public static final String ROOT_ACTOR_NAME = "reels-root";
}
