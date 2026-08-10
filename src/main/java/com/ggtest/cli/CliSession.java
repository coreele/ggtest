package com.ggtest.cli;

import com.ggtest.parser.SqlLogicTestParser;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
}
