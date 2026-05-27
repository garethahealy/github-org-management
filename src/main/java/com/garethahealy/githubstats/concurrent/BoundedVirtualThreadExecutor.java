package com.garethahealy.githubstats.concurrent;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Virtual-thread executor that limits how many submitted tasks run concurrently.
 */
public final class BoundedVirtualThreadExecutor extends AbstractExecutorService {

    private final Semaphore permits;
    private final ExecutorService delegate;

    public BoundedVirtualThreadExecutor(int maxConcurrency) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be >= 1");
        }
        this.permits = new Semaphore(maxConcurrency);
        this.delegate = Executors.newVirtualThreadPerTaskExecutor();
    }

    public static ExecutorService create(int maxConcurrency) {
        return new BoundedVirtualThreadExecutor(maxConcurrency);
    }

    @Override
    public void execute(Runnable command) {
        delegate.execute(() -> runWithPermit(command));
    }

    private void runWithPermit(Runnable command) {
        try {
            permits.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(e);
        }
        try {
            command.run();
        } finally {
            permits.release();
        }
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}
