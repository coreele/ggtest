package com.ggtest.db.postgres;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

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
        String schema = "ggtest_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET search_path TO " + schema + ", pg_catalog");
        }
        return schema;
    }

    /**
     * Drops the isolation schema and all objects created inside it.
     *
     * @throws SQLException when DROP SCHEMA fails
     */
    public static void teardown(Connection connection, String schema) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(schema, "schema");
        if (!isSafeIdentifier(schema)) {
            throw new SQLException("refusing to drop unsafe schema name");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    private static boolean isSafeIdentifier(String name) {
        return name.matches("[a-z][a-z0-9_]*");
    }
}
