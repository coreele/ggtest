package com.ggtest.cli;

import com.ggtest.parser.SqlLogicTestParser;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Worker result bundling an outcome with the per-file elapsed duration and the
 * file's input index (needed because {@link ExecutorCompletionService} does not
 * expose which Future maps to which submitted task).
 */
record IndexedTimedOutcome(int index, FileOutcome outcome, long elapsedMs) {}

/**
 * Orchestrates multi-file CLI execution: path display, per-file timing,
 * {@link FileRunner} invocation, and {@link ReportWriter} output.
 *
 * <p>Exit codes: hard errors → {@code 2}; assertion failures only → {@code 1};
 * otherwise {@code 0}. Hard-error files still increment {@code TOTAL.failed}.
 *
 * <p>When {@code --halt} is enabled ({@link CliOptions#halt()}), the file loop
 * stops as soon as a file maps to the {@link FileBucket#FAILED} bucket (assertion
 * failure or hard error): later files are not opened, parsed, executed, or counted.
 * Exit-code priority is unchanged.
 */
final class CliSession {

    /** Minimum status-line path column width (aligned with Spec frozen samples ~60). */
    static final int STATUS_PATH_COLUMN_WIDTH = 60;

    private final CliOptions options;
    private final PrintStream out;
    private final PrintStream err;
    private final ReportStyle style;
    private final ReportWriter reportWriter;
    private final FileRunner fileRunner;

    CliSession(CliOptions options, PrintStream out, PrintStream err, boolean ansiEnabled) {
        this.options = Objects.requireNonNull(options, "options");
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
        this.style = new ReportStyle(ansiEnabled);
        this.reportWriter = new ReportWriter(out, style);
        this.fileRunner = new FileRunner(options, err, reportWriter);
    }

    /**
     * @return CLI exit code: 0 all passed, 1 assertion failures, 2 hard errors
     */
    int execute(List<Path> files) {
        if (options.parallel() > 1) {
            return executeParallel(files);
        }

        int totalPassed = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        int totalOverridden = 0;
        boolean hardError = false;
        List<String> failedPaths = new ArrayList<>();

        int pathWidth = STATUS_PATH_COLUMN_WIDTH;
        List<String> displays = new ArrayList<>(files.size());
        for (Path file : files) {
            String display = ReportWriter.relativePath(file);
            displays.add(display);
            pathWidth = Math.max(pathWidth, display.length());
        }

        SqlLogicTestParser parser = new SqlLogicTestParser();

        FileBucket lastBucket = null;
        for (int index = 0; index < files.size(); index++) {
            Path file = files.get(index);
            String display = displays.get(index);
            long started = System.nanoTime();
            FileOutcome outcome = fileRunner.run(parser, file, display);
            long elapsedMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
            hardError = hardError || outcome.hardError();
            lastBucket = outcome.bucket();

            switch (outcome.bucket()) {
                case PASSED -> {
                    reportWriter.printStatusLine(display, pathWidth, style.passedTag(), elapsedMs, true);
                    totalPassed++;
                }
                case SKIPPED -> {
                    reportWriter.printStatusLine(display, pathWidth, style.skippedTag(), elapsedMs, false);
                    totalSkipped++;
                }
                case OVERRIDDEN -> {
                    reportWriter.printStatusLine(display, pathWidth, style.overriddenTag(), elapsedMs, true);
                    totalOverridden++;
                }
                case FAILED -> {
                    reportWriter.printStatusLine(display, pathWidth, style.failedTag(), elapsedMs, true);
                    for (String blockLine : outcome.detailLines()) {
                        out.println(blockLine);
                    }
                    out.println();
                    failedPaths.add(display);
                    totalFailed++;
                }
            }

            if (options.halt() && outcome.bucket() == FileBucket.FAILED) {
                break;
            }
        }

        reportWriter.printErrorSection(failedPaths, lastBucket);
        reportWriter.printTrailingBlankIfNeeded(failedPaths, totalPassed + totalOverridden, totalSkipped);
        reportWriter.printTotal(totalPassed, totalFailed, totalSkipped, totalOverridden, options.override());

        if (hardError) {
            return 2;
        }
        if (totalFailed > 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Parallel execution with controlled dispatch.
     *
     * <p>Tasks are submitted lazily: at most {@code parallel} run at once, and a new
     * task is dispatched only when a slot frees <em>and</em> halt has not tripped. This
     * makes the spec's {@code --halt} contract deterministic: a file is either
     * dispatched (runs to completion and is reported by its true bucket) or never
     * dispatched (not opened, not parsed, not counted) — there is no thread-grab race.
     */
    private int executeParallel(List<Path> files) {
        int parallelism = options.parallel();

        int pathWidth = STATUS_PATH_COLUMN_WIDTH;
        List<String> displays = new ArrayList<>(files.size());
        for (Path file : files) {
            String display = ReportWriter.relativePath(file);
            displays.add(display);
            pathWidth = Math.max(pathWidth, display.length());
        }

        SqlLogicTestParser parser = new SqlLogicTestParser();
        ParallelExecutor pool = new ParallelExecutor(parallelism);
        ExecutorCompletionService<IndexedTimedOutcome> ecs =
                new ExecutorCompletionService<>(pool.executor());

        List<Callable<IndexedTimedOutcome>> tasks = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            final int index = i;
            final Path file = files.get(i);
            final String display = displays.get(i);
            tasks.add(() -> {
                try {
                    FileRunner runner = new FileRunner(options, err, reportWriter);
                    long start = System.nanoTime();
                    FileOutcome outcome = runner.run(parser, file, display);
                    long ms = Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
                    return new IndexedTimedOutcome(index, outcome, ms);
                } catch (Throwable t) {
                    String w = t.getMessage() == null ? t.toString() : t.getMessage();
                    FileOutcome hard = FileOutcome.hardFailure(reportWriter.detailLines(
                            "unexpected error: " + w, null, display, null));
                    return new IndexedTimedOutcome(index, hard, 0L);
                }
            });
        }

        IndexedTimedOutcome[] results = new IndexedTimedOutcome[files.size()];
        Deque<Integer> pending = new ArrayDeque<>();
        for (int i = 0; i < files.size(); i++) {
            pending.addLast(i);
        }

        int running = 0;
        boolean halted = false;

        // Initial dispatch: fill up to `parallelism` slots.
        while (running < parallelism && !pending.isEmpty() && !halted) {
            ecs.submit(tasks.get(pending.removeFirst()));
            running++;
        }

        // Reap completions, tripping halt on the first FAILED, and refilling only
        // while halt has not tripped. Reaping continues until every dispatched task
        // has completed (we never cancel/interrupt running DB work).
        while (running > 0) {
            Future<IndexedTimedOutcome> future;
            try {
                future = ecs.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            running--;
            IndexedTimedOutcome result;
            try {
                result = future.get();
            } catch (Exception e) {
                // Callable catches Throwable, so this is unreachable; stay defensive.
                String w = e.getMessage() == null ? e.toString() : e.getMessage();
                FileOutcome hard = FileOutcome.hardFailure(reportWriter.detailLines(
                        "unexpected error: " + w, null, "?", null));
                result = new IndexedTimedOutcome(-1, hard, 0L);
            }
            if (result.index() >= 0) {
                results[result.index()] = result;
            }
            if (!halted && options.halt() && result.outcome().bucket() == FileBucket.FAILED) {
                halted = true;
            }
            while (running < parallelism && !pending.isEmpty() && !halted) {
                ecs.submit(tasks.get(pending.removeFirst()));
                running++;
            }
        }

        // Report collected results in input (sorted) order; never-dispatched files
        // (results[i] == null) are skipped entirely — no status line, no TOTAL count.
        int totalPassed = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        int totalOverridden = 0;
        boolean hardError = false;
        List<String> failedPaths = new ArrayList<>();
        FileBucket lastBucket = null;

        for (int i = 0; i < files.size(); i++) {
            IndexedTimedOutcome timed = results[i];
            if (timed == null) {
                continue;
            }
            String display = displays.get(i);
            FileOutcome outcome = timed.outcome();
            long elapsedMs = timed.elapsedMs();
            hardError = hardError || outcome.hardError();
            lastBucket = outcome.bucket();

            switch (outcome.bucket()) {
                case PASSED -> {
                    reportWriter.printStatusLine(display, pathWidth, style.passedTag(), elapsedMs, true);
                    totalPassed++;
                }
                case SKIPPED -> {
                    reportWriter.printStatusLine(display, pathWidth, style.skippedTag(), elapsedMs, false);
                    totalSkipped++;
                }
                case OVERRIDDEN -> {
                    reportWriter.printStatusLine(display, pathWidth, style.overriddenTag(), elapsedMs, true);
                    totalOverridden++;
                }
                case FAILED -> {
                    reportWriter.printStatusLine(display, pathWidth, style.failedTag(), elapsedMs, true);
                    for (String blockLine : outcome.detailLines()) {
                        out.println(blockLine);
                    }
                    out.println();
                    failedPaths.add(display);
                    totalFailed++;
                }
            }
        }

        reportWriter.printErrorSection(failedPaths, lastBucket);
        reportWriter.printTrailingBlankIfNeeded(failedPaths, totalPassed, totalSkipped);
        reportWriter.printTotal(totalPassed, totalFailed, totalSkipped, totalOverridden, options.override());

        pool.shutdown();
        try {
            pool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (hardError) {
            return 2;
        }
        if (totalFailed > 0) {
            return 1;
        }
        return 0;
    }
}
