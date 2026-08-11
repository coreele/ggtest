package com.ggtest.cli;

import com.ggtest.db.DatabaseExecutor;
import com.ggtest.model.SqlTestRecord;
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Runs a single test file: parse, JDBC lifecycle, runner invocation, outcome mapping. */
final class FileRunner {

    private final CliOptions options;
    private final PrintStream err;
    private final ReportWriter reportWriter;
    private final OverrideCoordinator overrideCoordinator;

    FileRunner(CliOptions options, PrintStream err, ReportWriter reportWriter) {
        this.options = Objects.requireNonNull(options, "options");
        this.err = Objects.requireNonNull(err, "err");
        this.reportWriter = Objects.requireNonNull(reportWriter, "reportWriter");
        this.overrideCoordinator = new OverrideCoordinator(reportWriter, this::sanitize);
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

        try (Connection connection = ConnectionFactory.open(options)) {
            EngineAdapter adapter = selectAdapter();
            return adapter.run(
                    connection,
                    executor -> runWithExecutor(executor, records, display, file),
                    display,
                    err,
                    reportWriter,
                    this::sanitize);
        } catch (SQLException ex) {
            err.println("connection failed: " + sanitize(ex.getMessage()));
            return FileOutcome.hardFailure(reportWriter.detailLines(
                    "connection failed: " + sanitize(ex.getMessage()),
                    null,
                    display,
                    null));
        }
    }

    private EngineAdapter selectAdapter() {
        if (RuntimeConfigResolver.ENGINE_POSTGRES.equals(options.engine())) {
            return new PostgresAdapter();
        }
        return new SqliteAdapter();
    }

    private FileOutcome runWithExecutor(
            DatabaseExecutor executor, List<SqlTestRecord> records, String display, Path file) {
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
            List<OverrideWriter.Override> overrides = overrideCoordinator.collectOverrides(result);
            if (!overrides.isEmpty()) {
                FileOutcome writeOutcome = overrideCoordinator.applyOverrideWriteBack(file, overrides, display);
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

    String sanitize(String message) {
        return CredentialRedaction.redactMessage(message, options.password());
    }
}
