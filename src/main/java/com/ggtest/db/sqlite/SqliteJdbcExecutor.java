package com.ggtest.db.sqlite;

import com.ggtest.db.AbstractJdbcExecutor;
import com.ggtest.db.DatabaseExecutor;
import java.sql.Connection;
import java.util.List;

/**
 * {@link DatabaseExecutor} backed by an open SQLite JDBC connection
 * ({@code org.xerial:sqlite-jdbc}), for example {@code jdbc:sqlite::memory:}.
 *
 * <p>The caller owns the connection: this class opens nothing, closes nothing,
 * and never initializes or cleans the database. Execution is serial on that one
 * connection, so all records of a file share the same session.
 *
 * <p>Values are returned exactly as {@code getString} yields them, with SQL NULL
 * as {@code null}; I/T/R normalization belongs to {@code com.ggtest.normalize}.
 */
public final class SqliteJdbcExecutor extends AbstractJdbcExecutor {

    /** Engine name matched by {@code skipif} / {@code onlyif} operands. */
    public static final String ENGINE_NAME = "sqlite";

    private static final List<String> FATAL_MESSAGE_MARKERS = List.of(
            "connection closed",
            "connection is closed",
            "connection has been closed",
            "database has been closed",
            "database connection closed",
            "out of memory",
            "no such database");

    /**
     * @param connection an already-open SQLite connection owned by the caller
     */
    public SqliteJdbcExecutor(Connection connection) {
        super(connection, FATAL_MESSAGE_MARKERS, "SQLite");
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }
}
