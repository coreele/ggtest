package com.ggtest.cli;

import com.ggtest.db.postgres.PostgresJdbcExecutor;
import com.ggtest.db.postgres.PostgresSchemaIsolation;
import com.ggtest.db.sqlite.SqliteJdbcExecutor;
import com.ggtest.model.QueryRecord;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.model.StatementRecord;
import com.ggtest.parser.ParseException;
import com.ggtest.parser.SqlLogicTestParser;
import com.ggtest.runner.FileRunResult;
import com.ggtest.runner.RecordOutcome;
import com.ggtest.runner.RecordResult;
import com.ggtest.runner.SqlLogicTestRunner;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Runs collected test files against a JDBC database and writes the human-readable
 * CLI report (relative paths, file-level status lines, inline failure blocks,
 * {@code Error:} list, and {@code TOTAL:}).
 *
 * <p>Each file gets an independent JDBC connection (opened, used, then closed)
 * and a fresh {@link SqlLogicTestRunner}. For {@code postgres}, a unique schema
 * is created, {@code search_path} is set, and the schema is dropped in
 * {@code finally} so user objects cannot leak across files.
 *
 * <p>Exit codes are independent of file counts: hard errors → {@code 2}; assertion
 * failures only → {@code 1}; otherwise {@code 0}. Hard-error files still increment
 * {@code TOTAL.failed}.
 */
final class CliSession {

    private static final Path CWD = Path.of("").toAbsolutePath().normalize();

    /** Minimum status-line path column width (aligned with Spec frozen samples ~60). */
    static final int STATUS_PATH_COLUMN_WIDTH = 60;

    private final CliOptions options;
    private final PrintStream out;
    private final PrintStream err;
    private final ReportStyle style;

    CliSession(CliOptions options, PrintStream out, PrintStream err, boolean ansiEnabled) {
        this.options = Objects.requireNonNull(options, "options");
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
        this.style = new ReportStyle(ansiEnabled);
    }

    /**
     * @return CLI exit code: 0 all passed, 1 assertion failures, 2 hard errors
     */
    int execute(List<Path> files) {
        int totalPassed = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        boolean hardError = false;
        List<String> failedPaths = new ArrayList<>();

        int pathWidth = STATUS_PATH_COLUMN_WIDTH;
        List<String> displays = new ArrayList<>(files.size());
        for (Path file : files) {
            String display = relativePath(file);
            displays.add(display);
            pathWidth = Math.max(pathWidth, display.length());
        }

        SqlLogicTestParser parser = new SqlLogicTestParser();

        FileBucket lastBucket = null;
        for (int index = 0; index < files.size(); index++) {
            Path file = files.get(index);
            String display = displays.get(index);
            long started = System.nanoTime();
            FileOutcome outcome = runOneFile(parser, file, display);
            long elapsedMs = Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
            hardError = hardError || outcome.hardError();
            lastBucket = outcome.bucket();

            switch (outcome.bucket()) {
                case PASSED -> {
                    printStatusLine(display, pathWidth, style.passedTag(), elapsedMs, true);
                    totalPassed++;
                }
                case SKIPPED -> {
                    printStatusLine(display, pathWidth, style.skippedTag(), elapsedMs, false);
                    totalSkipped++;
                }
                case FAILED -> {
                    printStatusLine(display, pathWidth, style.failedTag(), elapsedMs, true);
                    for (String blockLine : outcome.detailLines()) {
                        out.println(blockLine);
                    }
                    out.println();
                    failedPaths.add(display);
                    totalFailed++;
                }
            }
        }

        if (!failedPaths.isEmpty()) {
            // Failure blocks already end with a blank line; success/skip tails need one before Error.
            if (lastBucket != FileBucket.FAILED) {
                out.println();
            }
            out.println("Error: some test case failed:");
            out.println("[");
            for (String path : failedPaths) {
                out.println("    \"" + path + "\",");
            }
            out.println("]");
            out.println();
        } else if (totalPassed + totalSkipped > 0) {
            out.println();
        }

        out.printf("TOTAL: passed=%d failed=%d skipped=%d%n", totalPassed, totalFailed, totalSkipped);

        if (hardError) {
            return 2;
        }
        if (totalFailed > 0) {
            return 1;
        }
        return 0;
    }

    private void printStatusLine(String path, int pathWidth, String tag, long elapsedMs, boolean withTiming) {
        String padded = String.format("%-" + Math.max(pathWidth, 1) + "s", path);
        if (withTiming) {
            out.printf("%s .. %s in %d ms%n", padded, tag, elapsedMs);
        } else {
            out.printf("%s .. %s%n", padded, tag);
        }
    }

    private FileOutcome runOneFile(SqlLogicTestParser parser, Path file, String display) {
        List<SqlTestRecord> records;
        try {
            records = parser.parse(file);
        } catch (ParseException ex) {
            return FileOutcome.hardFailure(detailLines(
                    "parse error: " + sanitize(ex.reason()),
                    null,
                    null,
                    display,
                    ex.lineNumber()));
        } catch (IOException ex) {
            return FileOutcome.hardFailure(detailLines(
                    "io error: " + sanitize(ex.getMessage()),
                    null,
                    null,
                    display,
                    null));
        }

        try (Connection connection = openConnection()) {
            if (RuntimeConfigResolver.ENGINE_POSTGRES.equals(options.engine())) {
                return runPostgresFile(connection, records, display);
            }
            return runSqliteFile(connection, records, display);
        } catch (SQLException ex) {
            err.println("connection failed: " + sanitize(ex.getMessage()));
            return FileOutcome.hardFailure(detailLines(
                    "connection failed: " + sanitize(ex.getMessage()),
                    null,
                    null,
                    display,
                    null));
        }
    }

    private FileOutcome runSqliteFile(Connection connection, List<SqlTestRecord> records, String display) {
        SqliteJdbcExecutor executor = new SqliteJdbcExecutor(connection);
        return runWithExecutor(executor, records, display);
    }

    private FileOutcome runPostgresFile(Connection connection, List<SqlTestRecord> records, String display) {
        String schema = null;
        FileOutcome outcome = null;
        SQLException teardownException = null;
        try {
            schema = PostgresSchemaIsolation.prepare(connection);
            PostgresJdbcExecutor executor = new PostgresJdbcExecutor(connection);
            outcome = runWithExecutor(executor, records, display);
        } catch (SQLException ex) {
            err.println("schema isolation failed: " + sanitize(ex.getMessage()));
            return FileOutcome.hardFailure(detailLines(
                    "schema isolation failed: " + sanitize(ex.getMessage()),
                    null,
                    null,
                    display,
                    null));
        } finally {
            if (schema != null) {
                try {
                    PostgresSchemaIsolation.teardown(connection, schema);
                } catch (SQLException ex) {
                    err.println("schema teardown failed: " + sanitize(ex.getMessage()));
                    teardownException = ex;
                }
            }
        }
        if (teardownException != null) {
            List<String> details = new ArrayList<>(outcome.detailLines());
            if (details.isEmpty()) {
                details = detailLines(
                        "schema teardown failed: " + sanitize(teardownException.getMessage()),
                        null,
                        null,
                        display,
                        null);
            }
            return FileOutcome.hardFailure(details);
        }
        return outcome.hardError() ? FileOutcome.hardFailure(outcome.detailLines()) : outcome;
    }

    private FileOutcome runWithExecutor(
            com.ggtest.db.DatabaseExecutor executor, List<SqlTestRecord> records, String display) {
        SqlLogicTestRunner runner = new SqlLogicTestRunner(executor, options.hashThreshold());
        FileRunResult result = runner.run(records);

        List<String> detailLines = new ArrayList<>();
        for (RecordResult recordResult : result.recordResults()) {
            if (recordResult.outcome() == RecordOutcome.FAILED) {
                detailLines.addAll(formatFailure(display, recordResult));
            }
        }

        if (result.aborted()) {
            if (detailLines.isEmpty()) {
                detailLines = detailLines(
                        "aborted: " + sanitize(result.abortReason()),
                        null,
                        null,
                        display,
                        null);
            }
            return FileOutcome.hardFailure(detailLines);
        }

        if (result.failedCount() > 0) {
            return FileOutcome.assertionFailure(detailLines);
        }
        if (result.passedCount() == 0 && result.skippedCount() > 0) {
            return FileOutcome.skipped();
        }
        return FileOutcome.passed();
    }

    private List<String> formatFailure(String file, RecordResult recordResult) {
        String reason = recordResult.failureReason() == null ? "" : recordResult.failureReason();
        String why;
        String diffBody = null;
        if (reason.startsWith("result mismatch:")) {
            why = "query result mismatch:";
            String remainder = reason.substring("result mismatch:".length());
            if (remainder.startsWith("\n")) {
                remainder = remainder.substring(1);
            }
            if (!remainder.isBlank()) {
                diffBody = remainder;
            }
        } else {
            why = firstLine(reason);
            String rest = afterFirstLine(reason);
            if (!rest.isBlank() && looksLikeGitDiff(rest)) {
                diffBody = rest;
            }
        }
        return detailLines(why, sqlFirstLine(recordResult.record()), diffBody, file, recordResult.location().startLine());
    }

    private List<String> detailLines(
            String why, String sql, String diffBody, String file, Integer line) {
        List<String> lines = new ArrayList<>();
        lines.add("    " + style.label("[WHY]") + " " + why);
        if (sql != null && !sql.isBlank()) {
            lines.add("    " + style.label("[SQL]") + " " + sql);
        }
        if (diffBody != null && !diffBody.isBlank()) {
            lines.add("    " + style.label("[Diff]") + " (-expected|+actual)");
            for (String raw : diffBody.split("\\R", -1)) {
                if (raw.isEmpty()) {
                    continue;
                }
                lines.add("    " + colorDiffLine(raw));
            }
        }
        if (line != null) {
            lines.add("    at " + file + ":" + line);
        } else if (file != null) {
            lines.add("    at " + file);
        }
        return lines;
    }

    private String colorDiffLine(String raw) {
        if (raw.startsWith("-")) {
            return style.diffMinus(raw);
        }
        if (raw.startsWith("+")) {
            return style.diffPlus(raw);
        }
        return raw;
    }

    private Connection openConnection() throws SQLException {
        Properties properties = new Properties();
        options.user().ifPresent(user -> properties.setProperty("user", user));
        options.password().ifPresent(password -> properties.setProperty("password", password));
        if (properties.isEmpty()) {
            return DriverManager.getConnection(options.url());
        }
        return DriverManager.getConnection(options.url(), properties);
    }

    static String relativePath(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        try {
            if (absolute.startsWith(CWD)) {
                Path relative = CWD.relativize(absolute);
                String text = relative.toString();
                return text.isEmpty() ? absolute.getFileName().toString() : text;
            }
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        return absolute.toString();
    }

    private static String sqlFirstLine(SqlTestRecord record) {
        String sql;
        if (record instanceof StatementRecord statement) {
            sql = statement.sql();
        } else if (record instanceof QueryRecord query) {
            sql = query.sql();
        } else {
            sql = "";
        }
        if (sql == null || sql.isEmpty()) {
            return "";
        }
        int newline = sql.indexOf('\n');
        if (newline < 0) {
            return sql.stripTrailing();
        }
        String first = sql.substring(0, newline).stripTrailing();
        String remainder = sql.substring(newline + 1);
        if (!remainder.isBlank()) {
            return first + " ...";
        }
        return first;
    }

    private static String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int newline = text.indexOf('\n');
        return newline < 0 ? text.strip() : text.substring(0, newline).strip();
    }

    private static String afterFirstLine(String text) {
        if (text == null) {
            return "";
        }
        int newline = text.indexOf('\n');
        return newline < 0 ? "" : text.substring(newline + 1);
    }

    private static boolean looksLikeGitDiff(String text) {
        for (String line : text.split("\\R")) {
            if (line.startsWith("-   ") || line.startsWith("+   ") || line.startsWith("    ")) {
                return true;
            }
        }
        return false;
    }

    /** Strips accidental credential echoes; never logs passwords. */
    String sanitize(String message) {
        return CredentialRedaction.redactMessage(message, options.password());
    }

    private enum FileBucket {
        PASSED,
        FAILED,
        SKIPPED
    }

    private record FileOutcome(FileBucket bucket, boolean hardError, List<String> detailLines) {
        static FileOutcome passed() {
            return new FileOutcome(FileBucket.PASSED, false, List.of());
        }

        static FileOutcome skipped() {
            return new FileOutcome(FileBucket.SKIPPED, false, List.of());
        }

        static FileOutcome assertionFailure(List<String> detailLines) {
            return new FileOutcome(FileBucket.FAILED, false, List.copyOf(detailLines));
        }

        static FileOutcome hardFailure(List<String> detailLines) {
            return new FileOutcome(FileBucket.FAILED, true, List.copyOf(detailLines));
        }
    }
}
