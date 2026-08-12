package com.ggtest.cli;

import com.ggtest.db.DatabaseExecutor;
import com.ggtest.db.postgres.PostgresJdbcExecutor;
import com.ggtest.db.postgres.PostgresSchemaIsolation;
import com.ggtest.db.sqlite.SqliteJdbcExecutor;
import com.ggtest.db.mysql.MySqlJdbcExecutor;
import com.ggtest.db.mysql.MySqlSchemaIsolation;
import com.ggtest.db.xugu.XuguJdbcExecutor;
import com.ggtest.db.xugu.XuguSchemaIsolation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

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

        Map<String, Connection> connections = new HashMap<>();
        String[] fileSchemaHolder = {null};
        boolean isPostgres = RuntimeConfigResolver.ENGINE_POSTGRES.equals(options.engine());
        boolean isXugu = RuntimeConfigResolver.ENGINE_XUGU.equals(options.engine());
        boolean isMySql = RuntimeConfigResolver.ENGINE_MYSQL.equals(options.engine());
        boolean needsIsolation = isPostgres || isXugu || isMySql;

        if (needsIsolation) {
            try {
                Connection first = ConnectionFactory.open(options);
                connections.put("", first);
                try {
                    if (isPostgres) {
                        fileSchemaHolder[0] = PostgresSchemaIsolation.prepare(first);
                    } else if (isXugu) {
                        fileSchemaHolder[0] = XuguSchemaIsolation.prepare(first);
                    } else {
                        fileSchemaHolder[0] = MySqlSchemaIsolation.prepare(first);
                    }
                } catch (SQLException ex) {
                    err.println("schema isolation failed: " + sanitize(ex.getMessage()));
                    try {
                        first.close();
                    } catch (SQLException ignored) {
                    }
                    return FileOutcome.hardFailure(reportWriter.detailLines(
                            "schema isolation failed: " + sanitize(ex.getMessage()),
                            null, display, null));
                }
            } catch (SQLException ex) {
                err.println("connection failed: " + sanitize(ex.getMessage()));
                return FileOutcome.hardFailure(reportWriter.detailLines(
                        "connection failed: " + sanitize(ex.getMessage()),
                        null, display, null));
            }
        }
        String fileSchema = fileSchemaHolder[0];

        Function<String, DatabaseExecutor> factory = connKey -> {
            if (needsIsolation && "".equals(connKey)) {
                if (isPostgres) return new PostgresJdbcExecutor(connections.get(""));
                if (isXugu) return new XuguJdbcExecutor(connections.get(""));
                return new MySqlJdbcExecutor(connections.get(""));
            }
            try {
                Connection c = ConnectionFactory.open(options);
                connections.put(connKey, c);
                if (isPostgres) {
                    PostgresSchemaIsolation.setSearchPath(c, fileSchema);
                    return new PostgresJdbcExecutor(c);
                }
                if (isXugu) {
                    XuguSchemaIsolation.setSearchPath(c, fileSchema);
                    return new XuguJdbcExecutor(c);
                }
                if (isMySql) {
                    MySqlSchemaIsolation.setSearchPath(c, fileSchema);
                    return new MySqlJdbcExecutor(c);
                }
                return new SqliteJdbcExecutor(c);
            } catch (SQLException ex) {
                throw new com.ggtest.db.FatalDatabaseException(
                        "connection failed for conn '" + connKey + "': " + sanitize(ex.getMessage()), ex);
            }
        };

        try {
            SqlLogicTestRunner runner = new SqlLogicTestRunner(
                    factory, options.hashThreshold(), options.halt(), options.override());
            runner.setTraceStream(options.trace() ? err : null);
            FileRunResult result = runner.run(records);
            return processResult(result, display, file);
        } catch (com.ggtest.db.FatalDatabaseException ex) {
            err.println("connection failed: " + sanitize(ex.getMessage()));
            return FileOutcome.hardFailure(reportWriter.detailLines(
                    "connection failed: " + sanitize(ex.getMessage()),
                    null, display, null));
        } finally {
            if (fileSchema != null && connections.containsKey("")) {
                try {
                    if (isPostgres) PostgresSchemaIsolation.teardown(connections.get(""), fileSchema);
                    else if (isXugu) XuguSchemaIsolation.teardown(connections.get(""), fileSchema);
                    else MySqlSchemaIsolation.teardown(connections.get(""), fileSchema);
                } catch (SQLException ex) {
                    err.println("schema teardown failed: " + sanitize(ex.getMessage()));
                }
            }
            for (Connection c : connections.values()) {
                try { c.close(); } catch (SQLException ignored) { }
            }
        }
    }

    private FileOutcome processResult(FileRunResult result, String display, Path file) {
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
