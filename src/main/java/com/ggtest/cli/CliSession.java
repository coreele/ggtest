package com.ggtest.cli;

import com.ggtest.parser.SqlLogicTestParser;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Worker result bundling an outcome with the per-file elapsed duration.
 */
record TimedFileOutcome(FileOutcome outcome, long elapsedMs) {}

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

    private int executeParallel(List<Path> files) {
        int totalPassed = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
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
        ParallelExecutor executor = new ParallelExecutor(options.parallel());
        List<Callable<TimedFileOutcome>> tasks = new ArrayList<>(files.size());

        for (int i = 0; i < files.size(); i++) {
            Path file = files.get(i);
            String display = displays.get(i);
            tasks.add(() -> {
                FileRunner runner = new FileRunner(options, err, reportWriter);
                long start = System.nanoTime();
                FileOutcome outcome = runner.run(parser, file, display);
                long ms = Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
                return new TimedFileOutcome(outcome, ms);
            });
        }

        List<Future<TimedFileOutcome>> futures = executor.submitAll(tasks);
        FileBucket lastBucket = null;
        boolean halted = false;

        for (int i = 0; i < futures.size(); i++) {
            if (halted && futures.get(i).isCancelled()) {
                continue;
            }
            TimedFileOutcome timed;
            try {
                timed = futures.get(i).get();
            } catch (ExecutionException e) {
                String w = e.getCause() == null ? e.getMessage() : e.getCause().getMessage();
                timed = new TimedFileOutcome(FileOutcome.hardFailure(reportWriter.detailLines(
                        "unexpected error: " + w, null, displays.get(i), null)), 0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                timed = new TimedFileOutcome(FileOutcome.hardFailure(reportWriter.detailLines(
                        "interrupted", null, displays.get(i), null)), 0);
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

            if (!halted && options.halt() && outcome.bucket() == FileBucket.FAILED) {
                for (int j = i + 1; j < futures.size(); j++) {
                    futures.get(j).cancel(false);
                }
                halted = true;
            }
        }

        reportWriter.printErrorSection(failedPaths, lastBucket);
        reportWriter.printTrailingBlankIfNeeded(failedPaths, totalPassed, totalSkipped);
        reportWriter.printTotal(totalPassed, totalFailed, totalSkipped, 0, options.override());

        executor.shutdown();
        try {
            executor.awaitTermination(30, TimeUnit.SECONDS);
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
