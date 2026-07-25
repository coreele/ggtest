package com.ggtest.db.sqlite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.db.FatalDatabaseException;
import com.ggtest.db.QueryResult;
import com.ggtest.db.StatementResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SqliteJdbcExecutorTest {

    private Connection connection;
    private SqliteJdbcExecutor executor;

    @BeforeEach
    void openConnection() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        executor = new SqliteJdbcExecutor(connection);
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void engineNameIsSqlite() {
        assertEquals("sqlite", executor.engineName());
    }

    @Test
    void statementsRunAgainstTheSuppliedConnection() {
        assertTrue(executor.executeStatement("CREATE TABLE t1(a INTEGER, b TEXT)").succeeded());
        assertTrue(executor.executeStatement("INSERT INTO t1 VALUES(1, 'x')").succeeded());

        QueryResult result = executor.executeQuery("SELECT a, b FROM t1");

        assertTrue(result.succeeded());
        assertEquals(List.of(List.of("1", "x")), result.rows());
    }

    @Test
    void queryReturnsRawValuesAndSqlNullAsNull() {
        executor.executeStatement("CREATE TABLE t1(a INTEGER, b TEXT)");
        executor.executeStatement("INSERT INTO t1 VALUES(1, NULL)");

        QueryResult result = executor.executeQuery("SELECT a, b FROM t1");

        assertTrue(result.succeeded());
        assertEquals(1, result.rows().size());
        assertEquals("1", result.rows().get(0).get(0));
        assertNull(result.rows().get(0).get(1));
    }

    @Test
    void queryPreservesRowOrderAndColumnCount() {
        executor.executeStatement("CREATE TABLE t1(a INTEGER)");
        executor.executeStatement("INSERT INTO t1 VALUES(2),(1)");

        QueryResult result = executor.executeQuery("SELECT a FROM t1");

        assertEquals(List.of(List.of("2"), List.of("1")), result.rows());
    }

    @Test
    void rejectedStatementIsBusinessFailure() {
        StatementResult result = executor.executeStatement("INSERT INTO missing VALUES(1)");

        assertFalse(result.succeeded());
        assertFalse(result.errorSummary().isEmpty());
    }

    @Test
    void rejectedQueryIsBusinessFailure() {
        QueryResult result = executor.executeQuery("SELECT a FROM missing");

        assertFalse(result.succeeded());
        assertTrue(result.rows().isEmpty());
        assertFalse(result.errorSummary().isEmpty());
    }

    @Test
    void nonQuerySqlPassedToExecuteQueryIsNotFatal() {
        assertDoesNotThrow(() -> executor.executeQuery("CREATE TABLE t2(a INTEGER)"));
    }

    @Test
    void closedConnectionIsFatalForStatements() throws SQLException {
        connection.close();

        assertThrows(FatalDatabaseException.class, () -> executor.executeStatement("CREATE TABLE t1(a INTEGER)"));
    }

    @Test
    void closedConnectionIsFatalForQueries() throws SQLException {
        connection.close();

        assertThrows(FatalDatabaseException.class, () -> executor.executeQuery("SELECT 1"));
    }

    @Test
    void connectionIsNotClosedByTheExecutor() throws SQLException {
        executor.executeStatement("CREATE TABLE t1(a INTEGER)");
        executor.executeQuery("SELECT a FROM t1");

        assertFalse(connection.isClosed());
    }
}
