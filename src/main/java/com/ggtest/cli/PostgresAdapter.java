package com.ggtest.cli;

import com.ggtest.db.DatabaseExecutor;
import com.ggtest.db.postgres.PostgresJdbcExecutor;
import com.ggtest.db.postgres.PostgresSchemaIsolation;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** PostgreSQL engine adapter with per-file schema isolation. Package-private. */
final class PostgresAdapter implements EngineAdapter {

    @Override
    public String engineName() {
        return PostgresJdbcExecutor.ENGINE_NAME;
    }

    @Override
    public FileOutcome run(
            Connection connection,
            Function<DatabaseExecutor, FileOutcome> execute,
            String display,
            PrintStream err,
            ReportWriter reportWriter,
            Function<String, String> sanitize) throws SQLException {
        String schema = null;
        FileOutcome outcome = null;
        SQLException teardownException = null;
        try {
            schema = PostgresSchemaIsolation.prepare(connection);
            PostgresJdbcExecutor executor = new PostgresJdbcExecutor(connection);
            outcome = execute.apply(executor);
        } catch (SQLException ex) {
            err.println("schema isolation failed: " + sanitize.apply(ex.getMessage()));
            return FileOutcome.hardFailure(reportWriter.detailLines(
                    "schema isolation failed: " + sanitize.apply(ex.getMessage()),
                    null,
                    display,
                    null));
        } finally {
            if (schema != null) {
                try {
                    PostgresSchemaIsolation.teardown(connection, schema);
                } catch (SQLException ex) {
                    err.println("schema teardown failed: " + sanitize.apply(ex.getMessage()));
                    teardownException = ex;
                }
            }
        }
        if (teardownException != null) {
            List<String> details = new ArrayList<>(outcome.detailLines());
            if (details.isEmpty()) {
                details = reportWriter.detailLines(
                        "schema teardown failed: " + sanitize.apply(teardownException.getMessage()),
                        null,
                        display,
                        null);
            }
            return FileOutcome.hardFailure(details);
        }
        return outcome.hardError() ? FileOutcome.hardFailure(outcome.detailLines()) : outcome;
    }
}
