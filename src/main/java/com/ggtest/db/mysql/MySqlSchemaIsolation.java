package com.ggtest.db.mysql;

import com.ggtest.db.SchemaNames;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Per-file schema isolation helpers for MySQL.
 *
 * <p>MySQL {@code CREATE SCHEMA} is an alias for {@code CREATE DATABASE};
 * {@code USE} switches the default database for the session.
 * {@code DROP SCHEMA IF EXISTS} cascades automatically (no {@code CASCADE} keyword).
 */
public final class MySqlSchemaIsolation {

    private MySqlSchemaIsolation() {}

    public static String prepare(Connection connection) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        String schema = SchemaNames.generate();
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            statement.execute("USE " + schema);
        }
        return schema;
    }

    /**
     * @throws SQLException when DROP SCHEMA fails, or if {@code schema} is not a
     *     safe identifier
     */
    public static void teardown(Connection connection, String schema) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        SchemaNames.requireSafe(schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema);
        }
    }

    /**
     * @throws SQLException when USE fails, or if {@code schema} is not a safe
     *     identifier
     */
    public static void setSearchPath(Connection connection, String schema) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        SchemaNames.requireSafe(schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute("USE " + schema);
        }
    }
}
