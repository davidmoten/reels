package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

public class NewThreadWorker extends ExecutorWorker {

    public NewThreadWorker(ThreadFactory threadFactory) {
        super(createExecutor(threadFactory));
    }
    
    private static ScheduledExecutorService createExecutor(ThreadFactory factory) {
        final ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, factory);
        exec.setRemoveOnCancelPolicy(true);
        return exec;
    }
}
