package com.github.davidmoten.reels;

import java.util.concurrent.TimeUnit;

public interface Worker extends Disposable {
    
    Disposable schedule(Runnable run);

    Disposable schedule(Runnable run, long delay, TimeUnit unit);

    Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit);
}
