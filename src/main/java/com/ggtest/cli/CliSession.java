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
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * Runs collected test files against a JDBC database and writes the CLI report.
 *
 * <p>Each file gets an independent JDBC connection (opened, used, then closed)
 * and a fresh {@link SqlLogicTestRunner}. For {@code postgres}, a unique schema
 * is created, {@code search_path} is set, and the schema is dropped in
 * {@code finally} so user objects cannot leak across files.
 */
final class CliSession {

    private final CliOptions options;
    private final PrintStream out;
    private final PrintStream err;

    CliSession(CliOptions options, PrintStream out, PrintStream err) {
        this.options = Objects.requireNonNull(options, "options");
        this.out = Objects.requireNonNull(out, "out");
        this.err = Objects.requireNonNull(err, "err");
    }

    /**
     * @return CLI exit code: 0 all passed, 1 assertion failures, 2 hard errors
     */
    int execute(List<Path> files) {
        int totalPassed = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        boolean hardError = false;

        SqlLogicTestParser parser = new SqlLogicTestParser();

        for (Path file : files) {
            FileOutcome outcome = runOneFile(parser, file);
            hardError = hardError || outcome.hardError();
            totalPassed += outcome.passed();
            totalFailed += outcome.failed();
            totalSkipped += outcome.skipped();
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

    private FileOutcome runOneFile(SqlLogicTestParser parser, Path file) {
        String display = file.toString();
        List<SqlTestRecord> records;
        try {
            records = parser.parse(file);
        } catch (ParseException ex) {
            out.printf(
                    "ERROR: file=%s line=%d reason=%s%n",
                    display, ex.lineNumber(), ex.reason());
            out.printf("FILE: %s passed=0 failed=0 skipped=0 (parse error)%n", display);
            return FileOutcome.forHardError();
        } catch (IOException ex) {
            out.printf("ERROR: file=%s reason=%s%n", display, sanitize(ex.getMessage()));
            out.printf("FILE: %s passed=0 failed=0 skipped=0 (io error)%n", display);
            return FileOutcome.forHardError();
        }

        try (Connection connection = openConnection()) {
            if (RuntimeConfigResolver.ENGINE_POSTGRES.equals(options.engine())) {
                return runPostgresFile(connection, records, display);
            }
            return runSqliteFile(connection, records, display);
        } catch (SQLException ex) {
            err.println("connection failed: " + sanitize(ex.getMessage()));
            out.printf("FILE: %s passed=0 failed=0 skipped=0 (connection error)%n", display);
            return FileOutcome.forHardError();
        }
    }

    private FileOutcome runSqliteFile(Connection connection, List<SqlTestRecord> records, String display) {
        SqliteJdbcExecutor executor = new SqliteJdbcExecutor(connection);
        return runWithExecutor(executor, records, display);
    }

    private FileOutcome runPostgresFile(Connection connection, List<SqlTestRecord> records, String display) {
        String schema = null;
        try {
            schema = PostgresSchemaIsolation.prepare(connection);
            PostgresJdbcExecutor executor = new PostgresJdbcExecutor(connection);
            FileOutcome outcome = runWithExecutor(executor, records, display);
            try {
                PostgresSchemaIsolation.teardown(connection, schema);
                schema = null;
            } catch (SQLException ex) {
                err.println("schema teardown failed: " + sanitize(ex.getMessage()));
                return new FileOutcome(outcome.passed(), outcome.failed(), outcome.skipped(), true);
            }
            return outcome;
        } catch (SQLException ex) {
            err.println("schema isolation failed: " + sanitize(ex.getMessage()));
            out.printf("FILE: %s passed=0 failed=0 skipped=0 (isolation error)%n", display);
            return FileOutcome.forHardError();
        } finally {
            if (schema != null) {
                try {
                    PostgresSchemaIsolation.teardown(connection, schema);
                } catch (SQLException ex) {
                    err.println("schema teardown failed: " + sanitize(ex.getMessage()));
                }
            }
        }
    }

    private FileOutcome runWithExecutor(
            com.ggtest.db.DatabaseExecutor executor, List<SqlTestRecord> records, String display) {
        SqlLogicTestRunner runner = new SqlLogicTestRunner(executor, options.hashThreshold());
        FileRunResult result = runner.run(records);

        out.printf(
                "FILE: %s passed=%d failed=%d skipped=%d%n",
                display, result.passedCount(), result.failedCount(), result.skippedCount());

        for (RecordResult recordResult : result.recordResults()) {
            if (recordResult.outcome() == RecordOutcome.FAILED) {
                printFailure(display, recordResult);
            }
        }

        if (result.aborted()) {
            out.printf("ERROR: file=%s reason=%s%n", display, sanitize(result.abortReason()));
            return new FileOutcome(
                    result.passedCount(), result.failedCount(), result.skippedCount(), true);
        }
        return new FileOutcome(
                result.passedCount(), result.failedCount(), result.skippedCount(), false);
    }

    private void printFailure(String file, RecordResult recordResult) {
        out.printf(
                "FAILURE: file=%s line=%d summary=%s reason=%s%n",
                file,
                recordResult.location().startLine(),
                sqlFirstLine(recordResult.record()),
                singleLine(recordResult.failureReason()));
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
        return newline < 0 ? sql.strip() : sql.substring(0, newline).strip();
    }

    private static String singleLine(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\r', ' ').replace('\n', ' ').strip();
    }

    /** Strips accidental credential echoes; never logs passwords. */
    private static String sanitize(String message) {
        return message == null ? "" : message.strip();
    }

    private record FileOutcome(int passed, int failed, int skipped, boolean hardError) {
        static FileOutcome forHardError() {
            return new FileOutcome(0, 0, 0, true);
        }
    }
}
