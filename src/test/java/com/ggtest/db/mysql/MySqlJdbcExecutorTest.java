package com.ggtest.db.mysql;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ggtest.db.FatalDatabaseException;
import com.ggtest.db.QueryResult;
import com.ggtest.db.StatementResult;
import java.sql.*;
import java.util.*;
import org.junit.jupiter.api.*;

class MySqlJdbcExecutorTest {

    private Connection connection;
    private MySqlJdbcExecutor executor;

    @BeforeEach
    void openConnection() throws SQLException {
        String url = System.getenv("GGTEST_MY_URL");
        assumeTrue(url != null && !url.isBlank(), "GGTEST_MY_URL not set");

        Properties p = new Properties();
        var u = System.getenv("GGTEST_MY_USER"); if (u != null && !u.isBlank()) p.setProperty("user", u);
        var pw = System.getenv("GGTEST_MY_PASSWORD"); if (pw != null) p.setProperty("password", pw);
        connection = DriverManager.getConnection(url, p);
        try (var s = connection.createStatement()) {
            s.execute("CREATE SCHEMA IF NOT EXISTS ggtest_exec");
            s.execute("USE ggtest_exec");
        }
        executor = new MySqlJdbcExecutor(connection);
    }

    @AfterEach
    void close() throws SQLException { if (connection != null && !connection.isClosed()) connection.close(); }

    @Test void engineNameIsMysql() { assertEquals("mysql", executor.engineName()); }

    @Test void statementsRunAgainstSuppliedConnection() {
        executor.executeStatement("DROP TABLE IF EXISTS my_stmt");
        assertTrue(executor.executeStatement("CREATE TABLE my_stmt(a INTEGER, b VARCHAR(20))").succeeded());
        assertTrue(executor.executeStatement("INSERT INTO my_stmt VALUES(1, 'x')").succeeded());
        var r = executor.executeQuery("SELECT a, b FROM my_stmt");
        assertTrue(r.succeeded());
        assertEquals(List.of(List.of("1", "x")), r.rows());
    }

    @Test void queryReturnsNull() {
        executor.executeStatement("DROP TABLE IF EXISTS my_null");
        executor.executeStatement("CREATE TABLE my_null(a INTEGER)");
        executor.executeStatement("INSERT INTO my_null VALUES(NULL)");
        var r = executor.executeQuery("SELECT a FROM my_null");
        assertTrue(r.succeeded());
        assertEquals(1, r.rows().size());
        assertNull(r.rows().get(0).get(0));
    }

    @Test void rejectedStatementIsBusinessFailure() {
        var r = executor.executeStatement("INSERT INTO missing_xyz VALUES(1)");
        assertFalse(r.succeeded());
        assertFalse(r.errorSummary().isEmpty());
    }

    @Test void rejectedQueryIsBusinessFailure() {
        var r = executor.executeQuery("SELECT a FROM missing_xyz");
        assertFalse(r.succeeded());
        assertTrue(r.rows().isEmpty());
        assertFalse(r.errorSummary().isEmpty());
    }

    @Test void closedConnectionIsFatal() throws SQLException {
        connection.close();
        assertThrows(FatalDatabaseException.class, () -> executor.executeStatement("SELECT 1"));
        assertThrows(FatalDatabaseException.class, () -> executor.executeQuery("SELECT 1"));
    }

    @Test void connectionIsNotClosedByExecutor() throws SQLException {
        executor.executeStatement("DROP TABLE IF EXISTS my_keep");
        executor.executeStatement("CREATE TABLE my_keep(a INTEGER)");
        executor.executeQuery("SELECT a FROM my_keep");
        assertFalse(connection.isClosed());
    }
}
