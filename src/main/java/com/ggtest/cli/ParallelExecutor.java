package com.ggtest.cli;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class ParallelExecutor {
    private final ExecutorService executor;

    ParallelExecutor(int parallelism) {
        this.executor = Executors.newFixedThreadPool(parallelism);
    }

    /**
     * The underlying pool, so callers can wrap it (e.g. {@link java.util.concurrent.ExecutorCompletionService})
     * and control dispatch themselves. Dispatch — not lifecycle — is the caller's job.
     */
    ExecutorService executor() {
        return executor;
    }

    void shutdown() {
        executor.shutdown();
    }

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }
}
