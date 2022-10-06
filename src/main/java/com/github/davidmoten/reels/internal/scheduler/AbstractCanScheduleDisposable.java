package com.github.davidmoten.reels.internal.scheduler;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.reels.CanSchedule;
import com.github.davidmoten.reels.Disposable;

public abstract class AbstractCanScheduleDisposable implements CanSchedule, Disposable {

    @Override
    public Disposable schedule(Runnable run) {
        if (isDisposed()) {
            return Disposable.disposed();
        }
        return _schedule(run);
    }

    @Override
    public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
        if (delay <= 0) {
            return schedule(run);
        } else if (isDisposed()) {
            return Disposable.disposed();
        } else {
            return _schedule(run, delay, unit);
        }
    }

    @Override
    public Disposable schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit) {
        if (isDisposed()) {
            return Disposable.disposed();
        }
        return _schedulePeriodically(run, initialDelay, period, unit);
    }

    abstract protected Disposable _schedule(Runnable run);

    abstract protected Disposable _schedule(Runnable run, long delay, TimeUnit unit);

    abstract protected Disposable _schedulePeriodically(Runnable run, long initialDelay, long period, TimeUnit unit);

}
