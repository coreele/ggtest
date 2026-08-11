package com.ggtest.cli;

import com.ggtest.db.DatabaseExecutor;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Engine-specific lifecycle for executing a test file against a single connection.
 * Package-private; implementations handle engine routing and optional prepare/teardown.
 */
interface EngineAdapter {

    String engineName();

    FileOutcome run(
            Connection connection,
            Function<DatabaseExecutor, FileOutcome> execute,
            String display,
            PrintStream err,
            ReportWriter reportWriter,
            Function<String, String> sanitize) throws SQLException;
}
