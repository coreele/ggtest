package com.ggtest.db.mysql;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;

class MySqlSchemaIsolationTest {

    private Connection connection;

    @BeforeEach
    void open() throws SQLException {
        String url = System.getenv("GGTEST_MY_URL");
        assumeTrue(url != null && !url.isBlank());
        Properties p = new Properties();
        var u = System.getenv("GGTEST_MY_USER"); if (u != null && !u.isBlank()) p.setProperty("user", u);
        var pw = System.getenv("GGTEST_MY_PASSWORD"); if (pw != null) p.setProperty("password", pw);
        connection = DriverManager.getConnection(url, p);
    }

    @AfterEach
    void close() throws SQLException { if (connection != null && !connection.isClosed()) connection.close(); }

    @Test void objectsInOneSchemaLifecycleAreInvisibleInTheNext() throws SQLException {
        String first = MySqlSchemaIsolation.prepare(connection);
        try (Statement s = connection.createStatement()) {
            s.execute("CREATE TABLE probe1(id INTEGER)");
            s.execute("INSERT INTO probe1 VALUES (1)");
            assertTrue(tableExists("probe1"));
        } finally {
            MySqlSchemaIsolation.teardown(connection, first);
        }

        String second = MySqlSchemaIsolation.prepare(connection);
        try (Statement s = connection.createStatement()) {
            assertFalse(tableExists("probe1"));
            s.execute("CREATE TABLE probe1(id INTEGER)");
            assertTrue(tableExists("probe1"));
        } finally {
            MySqlSchemaIsolation.teardown(connection, second);
        }
    }

    private boolean tableExists(String table) {
        try (Statement s = connection.createStatement()) {
            s.executeQuery("SELECT 1 FROM " + table + " WHERE 1=0");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }
}
