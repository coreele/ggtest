package com.ggtest.db.postgres;

import com.ggtest.db.SchemaNames;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Per-file schema isolation helpers for PostgreSQL.
 *
 * <p>Not part of {@link com.ggtest.db.DatabaseExecutor}: the CLI orchestrates
 * prepare → run → teardown on the same caller-owned connection.
 */
public final class PostgresSchemaIsolation {

    private PostgresSchemaIsolation() {}

    /**
     * Creates a unique schema and sets {@code search_path} so unqualified names
     * resolve there (with {@code pg_catalog} retained).
     *
     * @return the created schema name (safe SQL identifier)
     * @throws SQLException when CREATE SCHEMA or SET search_path fails
     */
    public static String prepare(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String schema = SchemaNames.generate();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema + ", pg_catalog");
        }
        return schema;
    }

    /**
     * Drops the isolation schema and all objects created inside it.
     *
     * @throws SQLException when DROP SCHEMA fails, or if {@code schema} is not a
     *     safe identifier
     */
    public static void teardown(Connection connection, String schema) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        SchemaNames.requireSafe(schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    /**
     * Points an existing connection to an already-created isolation schema.
     * Used for additional connections in multi-connection (conn=<name>) mode.
     *
     * @throws SQLException when SET search_path fails, or if {@code schema} is
     *     not a safe identifier
     */
    public static void setSearchPath(Connection connection, String schema) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        SchemaNames.requireSafe(schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + schema + ", pg_catalog");
        }
    }
}
