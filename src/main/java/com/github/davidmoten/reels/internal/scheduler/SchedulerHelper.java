package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.ThreadFactory;

public final class SchedulerHelper {
    
    private SchedulerHelper() {
        // prevent instantiation
    }
    
    public static ThreadFactory createThreadFactory(String prefix) {
        return new PrefixedThreadFactory(prefix);
    }
}
