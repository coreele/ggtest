package com.ggtest.db.postgres;

import com.ggtest.db.AbstractJdbcExecutor;
import com.ggtest.db.DatabaseExecutor;
import java.sql.Connection;
import java.util.List;

/**
 * {@link DatabaseExecutor} backed by an open PostgreSQL JDBC connection
 * ({@code org.postgresql:postgresql}).
 *
 * <p>The caller owns the connection: this class opens nothing, closes nothing,
 * and never creates or drops schemas. Isolation is orchestrated by the CLI via
 * {@link PostgresSchemaIsolation}.
 *
 * <p>Values are returned exactly as {@code getString} yields them, with SQL NULL
 * as {@code null}; I/T/R normalization belongs to {@code com.ggtest.normalize}.
 */
public final class PostgresJdbcExecutor extends AbstractJdbcExecutor {

    /** Engine name matched by {@code skipif} / {@code onlyif} operands. */
    public static final String ENGINE_NAME = "postgres";

    private static final List<String> FATAL_MESSAGE_MARKERS = List.of(
            "connection closed",
            "connection is closed",
            "connection has been closed",
            "this connection has been closed",
            "connection has been terminated");

    /**
     * @param connection an already-open PostgreSQL connection owned by the caller
     */
    public PostgresJdbcExecutor(Connection connection) {
        super(connection, FATAL_MESSAGE_MARKERS, "PostgreSQL");
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }
}
