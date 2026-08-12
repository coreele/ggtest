package com.ggtest.db.xugu;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Per-file schema isolation helpers for XuguDB.
 *
 * <p>Not part of {@link com.ggtest.db.DatabaseExecutor}: the CLI orchestrates
 * prepare → run → teardown on the same caller-owned connection.
 *
 * <p>Mirrors {@code PostgresSchemaIsolation}: a unique schema is created per file,
 * {@code SET SCHEMA} routes unqualified names into it, and {@code DROP SCHEMA ...
 * CASCADE} tears it down. Xugu keeps the system catalog reachable after
 * {@code SET SCHEMA} (no {@code pg_catalog} fallback needed).
 */
public final class XuguSchemaIsolation {

    private XuguSchemaIsolation() {}

    /**
     * Creates a unique schema and points the session at it so unqualified names
     * resolve there.
     *
     * @return the created schema name (safe SQL identifier)
     * @throws SQLException when CREATE SCHEMA or SET SCHEMA fails
     */
    public static String prepare(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String schema = "ggtest_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA " + schema);
            statement.execute("SET SCHEMA " + schema);
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
            // Xugu's DROP SCHEMA does not accept IF EXISTS (unlike DROP TABLE);
            // the schema always exists at teardown (created in prepare()).
            statement.execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    /**
     * Points an existing connection to an already-created isolation schema.
     * Used for additional connections in multi-connection (conn=&lt;name&gt;) mode.
     *
     * @throws SQLException when SET SCHEMA fails
     */
    public static void setSearchPath(Connection connection, String schema) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(schema, "schema");
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET SCHEMA " + schema);
        }
    }

    private static boolean isSafeIdentifier(String name) {
        return name.matches("[a-z][a-z0-9_]*");
    }
}
