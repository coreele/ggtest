package com.ggtest.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

final class ParallelExecutor {
    private final ExecutorService executor;

    ParallelExecutor(int parallelism) {
        this.executor = Executors.newFixedThreadPool(parallelism);
    }

    <T> List<Future<T>> submitAll(List<Callable<T>> tasks) {
        List<Future<T>> futures = new ArrayList<>(tasks.size());
        for (Callable<T> task : tasks) {
            futures.add(executor.submit(task));
        }
        return futures;
    }

    void shutdown() {
        executor.shutdown();
    }

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return executor.awaitTermination(timeout, unit);
    }
}
