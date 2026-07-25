package com.ggtest.db.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ggtest.db.FatalDatabaseException;
import com.ggtest.db.QueryResult;
import com.ggtest.db.StatementResult;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * P0-PG-1: gated by {@code GGTEST_PG_URL} (optional user/password). Skips when unset.
 */
class PostgresJdbcExecutorTest {

    private Connection connection;
    private PostgresJdbcExecutor executor;

    @BeforeEach
    void openConnection() throws SQLException {
        String url = System.getenv("GGTEST_PG_URL");
        assumeTrue(url != null && !url.isBlank(), "GGTEST_PG_URL not set; skipping PG executor tests");

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
        executor = new PostgresJdbcExecutor(connection);
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void engineNameIsPostgres() {
        assertEquals("postgres", executor.engineName());
    }

    @Test
    void statementsRunAgainstTheSuppliedConnection() {
        assertTrue(executor.executeStatement("CREATE TEMP TABLE t1(a INTEGER, b TEXT)").succeeded());
        assertTrue(executor.executeStatement("INSERT INTO t1 VALUES(1, 'x')").succeeded());

        QueryResult result = executor.executeQuery("SELECT a, b FROM t1");

        assertTrue(result.succeeded());
        assertEquals(List.of(List.of("1", "x")), result.rows());
    }

    @Test
    void queryReturnsRawValuesAndSqlNullAsNull() {
        executor.executeStatement("CREATE TEMP TABLE t1(a INTEGER, b TEXT)");
        executor.executeStatement("INSERT INTO t1 VALUES(1, NULL)");

        QueryResult result = executor.executeQuery("SELECT a, b FROM t1");

        assertTrue(result.succeeded());
        assertEquals(1, result.rows().size());
        assertEquals("1", result.rows().get(0).get(0));
        assertNull(result.rows().get(0).get(1));
    }

    @Test
    void rejectedStatementIsBusinessFailure() {
        StatementResult result = executor.executeStatement("INSERT INTO missing_table_xyz VALUES(1)");

        assertFalse(result.succeeded());
        assertFalse(result.errorSummary().isEmpty());
    }

    @Test
    void rejectedQueryIsBusinessFailure() {
        QueryResult result = executor.executeQuery("SELECT a FROM missing_table_xyz");

        assertFalse(result.succeeded());
        assertTrue(result.rows().isEmpty());
        assertFalse(result.errorSummary().isEmpty());
    }

    @Test
    void closedConnectionIsFatalForStatements() throws SQLException {
        connection.close();

        assertThrows(
                FatalDatabaseException.class,
                () -> executor.executeStatement("CREATE TEMP TABLE t1(a INTEGER)"));
    }

    @Test
    void closedConnectionIsFatalForQueries() throws SQLException {
        connection.close();

        assertThrows(FatalDatabaseException.class, () -> executor.executeQuery("SELECT 1"));
    }

    @Test
    void connectionIsNotClosedByTheExecutor() throws SQLException {
        executor.executeStatement("CREATE TEMP TABLE t1(a INTEGER)");
        executor.executeQuery("SELECT a FROM t1");

        assertFalse(connection.isClosed());
    }
}
