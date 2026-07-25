package com.ggtest.db.postgres;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * P0-PG-4 helper evidence: two schema lifecycles keep user objects invisible across boundaries.
 * Gated by {@code GGTEST_PG_URL}.
 */
class PostgresSchemaIsolationTest {

    private Connection connection;

    @BeforeEach
    void openConnection() throws SQLException {
        String url = System.getenv("GGTEST_PG_URL");
        assumeTrue(url != null && !url.isBlank(), "GGTEST_PG_URL not set; skipping PG isolation tests");

        Properties properties = new Properties();
        String user = System.getenv("GGTEST_PG_USER");
        String password = System.getenv("GGTEST_PG_PASSWORD");
        if (user != null && !user.isBlank()) {
            properties.setProperty("user", user);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }
        connection = properties.isEmpty()
                ? DriverManager.getConnection(url)
                : DriverManager.getConnection(url, properties);
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void objectsInOneSchemaLifecycleAreInvisibleInTheNext() throws SQLException {
        String first = PostgresSchemaIsolation.prepare(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE isolation_probe(id INTEGER)");
            statement.execute("INSERT INTO isolation_probe VALUES (1)");
            assertTrue(tableExists(statement, "isolation_probe"));
        } finally {
            PostgresSchemaIsolation.teardown(connection, first);
        }

        String second = PostgresSchemaIsolation.prepare(connection);
        try (Statement statement = connection.createStatement()) {
            assertFalse(tableExists(statement, "isolation_probe"));
            statement.execute("CREATE TABLE isolation_probe(id INTEGER)");
            assertTrue(tableExists(statement, "isolation_probe"));
        } finally {
            PostgresSchemaIsolation.teardown(connection, second);
        }
    }

    private static boolean tableExists(Statement statement, String table) throws SQLException {
        try (ResultSet rs = statement.executeQuery(
                "SELECT EXISTS (SELECT 1 FROM pg_catalog.pg_class c "
                        + "JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace "
                        + "WHERE c.relkind = 'r' AND c.relname = '" + table + "' "
                        + "AND n.nspname = any(current_schemas(false)))")) {
            assertTrue(rs.next());
            return rs.getBoolean(1);
        }
    }
}
