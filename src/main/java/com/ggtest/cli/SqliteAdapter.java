package com.ggtest.cli;

import com.ggtest.db.DatabaseExecutor;
import com.ggtest.db.sqlite.SqliteJdbcExecutor;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/** SQLite engine adapter: no schema isolation needed. Package-private. */
final class SqliteAdapter implements EngineAdapter {

    @Override
    public String engineName() {
        return SqliteJdbcExecutor.ENGINE_NAME;
    }

    @Override
    public FileOutcome run(
            Connection connection,
            Function<DatabaseExecutor, FileOutcome> execute,
            String display,
            PrintStream err,
            ReportWriter reportWriter,
            Function<String, String> sanitize) throws SQLException {
        return execute.apply(new SqliteJdbcExecutor(connection));
    }
}
