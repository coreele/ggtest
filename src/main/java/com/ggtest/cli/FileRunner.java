package com.ggtest.cli;

import com.ggtest.db.postgres.PostgresJdbcExecutor;
import com.ggtest.db.postgres.PostgresSchemaIsolation;
import com.ggtest.db.sqlite.SqliteJdbcExecutor;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.parser.ParseException;
import com.ggtest.parser.SqlLogicTestParser;
import com.ggtest.runner.FileRunResult;
import com.ggtest.runner.RecordOutcome;
import com.ggtest.runner.RecordResult;
import com.ggtest.runner.SqlLogicTestRunner;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/** Runs a single test file: parse, JDBC lifecycle, runner invocation, outcome mapping. */
final class FileRunner {

    private final CliOptions options;
    private final PrintStream err;
    private final ReportWriter reportWriter;

    FileRunner(CliOptions options, PrintStream err, ReportWriter reportWriter) {
        this.options = Objects.requireNonNull(options, "options");
        this.err = Objects.requireNonNull(err, "err");
        this.reportWriter = Objects.requireNonNull(reportWriter, "reportWriter");
    }

    FileOutcome run(SqlLogicTestParser parser, Path file, String display) {
        List<SqlTestRecord> records;
        try {
            records = parser.parse(file);
        } catch (ParseException ex) {
            return FileOutcome.hardFailure(reportWriter.detailLines(
                    "parse error: " + sanitize(ex.reason()),
                    null,
                    display,
                    ex.lineNumber()));
        } catch (IOException ex) {
            return FileOutcome.hardFailure(reportWriter.detailLines(
                    "io error: " + sanitize(ex.getMessage()),
                    null,
                    display,
                    null));
        }

        try (Connection connection = openConnection()) {
            if (RuntimeConfigResolver.ENGINE_POSTGRES.equals(options.engine())) {
                return runPostgresFile(connection, records, display, file);
            }
            return runSqliteFile(connection, records, display, file);
        } catch (SQLException ex) {
            err.println("connection failed: " + sanitize(ex.getMessage()));
            return FileOutcome.hardFailure(reportWriter.detailLines(
                    "connection failed: " + sanitize(ex.getMessage()),
                    null,
                    display,
                    null));
        }
    }

    private FileOutcome runSqliteFile(Connection connection, List<SqlTestRecord> records, String display, Path file) {
        SqliteJdbcExecutor executor = new SqliteJdbcExecutor(connection);
        return runWithExecutor(executor, records, display, file);
    }

    private FileOutcome runPostgresFile(Connection connection, List<SqlTestRecord> records, String display, Path file) {
        String schema = null;
        FileOutcome outcome = null;
        SQLException teardownException = null;
        try {
            schema = PostgresSchemaIsolation.prepare(connection);
            PostgresJdbcExecutor executor = new PostgresJdbcExecutor(connection);
            outcome = runWithExecutor(executor, records, display, file);
        } catch (SQLException ex) {
            err.println("schema isolation failed: " + sanitize(ex.getMessage()));
            return FileOutcome.hardFailure(reportWriter.detailLines(
                    "schema isolation failed: " + sanitize(ex.getMessage()),
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
                details = reportWriter.detailLines(
                        "schema teardown failed: " + sanitize(teardownException.getMessage()),
                        null,
                        display,
                        null);
            }
            return FileOutcome.hardFailure(details);
        }
        return outcome.hardError() ? FileOutcome.hardFailure(outcome.detailLines()) : outcome;
    }

    private FileOutcome runWithExecutor(
            com.ggtest.db.DatabaseExecutor executor, List<SqlTestRecord> records, String display, Path file) {
        SqlLogicTestRunner runner = new SqlLogicTestRunner(
                executor, options.hashThreshold(), options.halt(), options.override());
        FileRunResult result = runner.run(records);

        List<String> detailLines = new ArrayList<>();
        for (RecordResult recordResult : result.recordResults()) {
            if (recordResult.outcome() == RecordOutcome.FAILED) {
                detailLines.addAll(reportWriter.formatFailureDetailLines(display, recordResult));
            }
        }

        if (result.aborted()) {
            if (detailLines.isEmpty()) {
                detailLines = reportWriter.detailLines(
                        "aborted: " + sanitize(result.abortReason()),
                        null,
                        display,
                        null);
            }
            return FileOutcome.hardFailure(detailLines);
        }

        if (options.override()) {
            List<OverrideWriter.Override> overrides = collectOverrides(result);
            if (!overrides.isEmpty()) {
                FileOutcome writeOutcome = applyOverrideWriteBack(file, overrides, display);
                if (writeOutcome != null) {
                    return writeOutcome;
                }
            }
        }

        if (result.failedCount() > 0) {
            return FileOutcome.assertionFailure(detailLines);
        }
        if (result.overriddenCount() > 0) {
            return FileOutcome.overridden();
        }
        if (result.passedCount() == 0 && result.skippedCount() > 0) {
            return FileOutcome.skipped();
        }
        return FileOutcome.passed();
    }

    private static List<OverrideWriter.Override> collectOverrides(FileRunResult result) {
        List<OverrideWriter.Override> overrides = new ArrayList<>();
        for (RecordResult rr : result.recordResults()) {
            if (rr.outcome() == RecordOutcome.OVERRIDDEN && rr.overrideText().isPresent()) {
                overrides.add(new OverrideWriter.Override(rr.record(), rr.overrideText().orElseThrow()));
            }
        }
        return overrides;
    }

    private FileOutcome applyOverrideWriteBack(
            Path file, List<OverrideWriter.Override> overrides, String display) {
        OverrideWriter overrideWriter = new OverrideWriter();
        try {
            String original = Files.readString(file, StandardCharsets.UTF_8);
            String rewritten = overrideWriter.rewrite(original, overrides);
            overrideWriter.writeAtomically(file, rewritten);
        } catch (IOException ex) {
            List<String> hardDetail = new ArrayList<>(reportWriter.detailLines(
                    "override write failed: " + sanitize(ex.getMessage()),
                    null,
                    display,
                    null));
            for (OverrideWriter.Override ov : overrides) {
                hardDetail.addAll(reportWriter.detailLines(
                        "would have overridden record at line " + ov.record().location().startLine(),
                        null,
                        display,
                        ov.record().location().startLine()));
            }
            return FileOutcome.hardFailure(hardDetail);
        }
        return null;
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

    String sanitize(String message) {
        return CredentialRedaction.redactMessage(message, options.password());
    }
}
