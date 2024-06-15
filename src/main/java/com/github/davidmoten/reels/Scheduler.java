package com.github.davidmoten.reels;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.github.davidmoten.reels.internal.Constants;
import com.github.davidmoten.reels.internal.scheduler.SchedulerComputationSticky;
import com.github.davidmoten.reels.internal.scheduler.SchedulerDoNothing;
import com.github.davidmoten.reels.internal.scheduler.SchedulerForkJoinPool;
import com.github.davidmoten.reels.internal.scheduler.SchedulerFromExecutor;
import com.github.davidmoten.reels.internal.scheduler.SchedulerImmediate;
import com.github.davidmoten.reels.internal.scheduler.SchedulerIo;
import com.github.davidmoten.reels.internal.scheduler.TestScheduler;

public interface Scheduler extends CanSchedule {

    /**
     * Returns a version of this Scheduler that is disposable (without affecting the
     * Scheduler itself). One disposed the Worker will not process any subsequent
     * tasks submitted to it.
     * 
     * @return a worker for this scheduler
     */
    Worker createWorker();

    /**
     * Returns true if and only if the draining and processing of messages from the
     * queue (mailbox) needs enforced synchronization (to ensure in-order
     * processing). For example the {@code io()} Scheduler always processes a single
     * actor's messages on the same thread so doesn't require multithreaded access
     * protection.
     * 
     * @return true true iff requires synchronization
     */
    boolean requiresDrainSynchronization();

    /**
     * Shuts this scheduler down so that subsequent tasks submitted to it will be
     * ignored.
     */
    void shutdown();

    static Scheduler defaultScheduler() {
        return forkJoin();
    }

    /**
     * Work-stealing Scheduler using the ForkJoin.common pool
     * 
     * @return Scheduler using the ForkJoin.common pool
     */
    static Scheduler forkJoin() {
        return SchedulerForkJoinPool.INSTANCE;
    }

    /**
     * Assigns tasks to a pool of threads whose size is the number of processors.
     * 
     * @return computation scheduler
     */
    static Scheduler computation() {
        return forkJoin();
    }

    /**
     * Assigns tasks to a pool of threads whose size is the number of processors. A
     * worker created by this scheduler will always run its work on the same thread
     * (chosen from the pool at the creation of the worker). As a consequence a
     * little less enforced serialization is required in the processing of buffered
     * messages (volatile reads still occur to check for disposal). In general the
     * {@code computation()} scheduler outperforms this one but there may be use
     * cases where it has an advantage.
     * 
     * @return computation scheduler of which a created worker always uses the same
     *         thread
     */
    static Scheduler computationSticky() {
        // outperforms NonSticky
        return SchedulerComputationSticky.INSTANCE;
    }

    /**
     * Use this scheduler for actors that perform blocking operations (like network
     * calls, database access, file system access).
     * 
     * @return io scheduler
     */
    static Scheduler io() {
        return SchedulerIo.INSTANCE;
    }

    /**
     * Runs all tasks on the current thread. Don't mix use of this scheduler with
     * other schedulers because message ordering will not be honoured (an immediate
     * scheduler running on two different threads will operate completely
     * independently). This scheduler is protected against stack overflow in the
     * case of recursive scheduling.
     * 
     * @return immediate scheduler
     */
    static Scheduler immediate() {
        return SchedulerImmediate.INSTANCE;
    }

    /**
     * Runs all tasks on a singleton thread.
     * 
     * @return single scheduler
     */
    static Scheduler single() {
        return Constants.SINGLE;
    }

    /**
     * Runs all tasks on a newly created single thread.
     * 
     * @return new thread single scheduler
     */
    static Scheduler newSingle() {
        return fromExecutor(Executors.newSingleThreadScheduledExecutor(Constants.NEW_SINGLE_THREAD_FACTORY),
                false);
    }

    static Scheduler fromExecutor(ScheduledExecutorService executor) {
        return new SchedulerFromExecutor(executor, true);
    }

    static Scheduler fromExecutor(ScheduledExecutorService executor, boolean requiresSerialization) {
        return new SchedulerFromExecutor(executor, requiresSerialization);
    }

    /**
     * Returns a scheduler that accepts all tasks but does nothing.
     * 
     * @return a scheduler that does not run submitted tasks
     */
    static Scheduler doNothing() {
        return SchedulerDoNothing.INSTANCE;
    }

    /**
     * Returns a scheduler that is useful for unit testing reels actors.
     * 
     * @return test scheduler
     */
    static TestScheduler test() {
        return new TestScheduler();
    }

}
