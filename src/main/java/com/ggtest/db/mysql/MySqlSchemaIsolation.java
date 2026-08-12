package com.ggtest.db.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

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
        String schema = "ggtest_" + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
            statement.execute("USE " + schema);
        }
        return schema;
    }

    public static void teardown(Connection connection, String schema) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(schema, "schema");
        if (!isSafeIdentifier(schema)) {
            throw new SQLException("refusing to drop unsafe schema name");
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema);
        }
    }

    public static void setSearchPath(Connection connection, String schema) throws SQLException {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(schema, "schema");
        try (Statement statement = connection.createStatement()) {
            statement.execute("USE " + schema);
        }
    }

    private static boolean isSafeIdentifier(String name) {
        return name.matches("[a-z][a-z0-9_]*");
    }
}
