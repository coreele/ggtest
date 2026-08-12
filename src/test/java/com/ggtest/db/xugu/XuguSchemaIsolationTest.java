package com.ggtest.db.xugu;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * P0-XG-iso: two schema lifecycles keep user objects invisible across boundaries.
 * Gated by {@code GGTEST_XG_URL}.
 */
class XuguSchemaIsolationTest {

    private Connection connection;

    @BeforeEach
    void openConnection() throws SQLException {
        String url = System.getenv("GGTEST_XG_URL");
        assumeTrue(url != null && !url.isBlank(), "GGTEST_XG_URL not set; skipping Xugu isolation tests");

        Properties properties = new Properties();
        String user = System.getenv("GGTEST_XG_USER");
        String password = System.getenv("GGTEST_XG_PASSWORD");
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
        String first = XuguSchemaIsolation.prepare(connection);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE isolation_probe(id INTEGER)");
            statement.execute("INSERT INTO isolation_probe VALUES (1)");
            assertTrue(tableExists("isolation_probe"));
        } finally {
            XuguSchemaIsolation.teardown(connection, first);
        }

        String second = XuguSchemaIsolation.prepare(connection);
        try (Statement statement = connection.createStatement()) {
            // Unqualified name resolves in the new schema; the probe from the first
            // lifecycle was dropped with schema 1 (CASCADE).
            assertFalse(tableExists("isolation_probe"));
            statement.execute("CREATE TABLE isolation_probe(id INTEGER)");
            assertTrue(tableExists("isolation_probe"));
        } finally {
            XuguSchemaIsolation.teardown(connection, second);
        }
    }

    private boolean tableExists(String table) {
        String sql = "SELECT 1 FROM " + table + " WHERE 1=0";
        try (Statement statement = connection.createStatement()) {
            statement.executeQuery(sql);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
