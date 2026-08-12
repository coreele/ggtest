package com.ggtest.db.xugu;

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
 * P0-XG-1: gated by {@code GGTEST_XG_URL} (optional user/password). Skips when unset.
 */
class XuguJdbcExecutorTest {

    private Connection connection;
    private XuguJdbcExecutor executor;

    @BeforeEach
    void openConnection() throws SQLException {
        String url = System.getenv("GGTEST_XG_URL");
        assumeTrue(url != null && !url.isBlank(), "GGTEST_XG_URL not set; skipping Xugu executor tests");

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
        executor = new XuguJdbcExecutor(connection);
    }

    @AfterEach
    void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void engineNameIsXugu() {
        assertEquals("xugu", executor.engineName());
    }

    @Test
    void statementsRunAgainstTheSuppliedConnection() {
        assertTrue(executor.executeStatement("DROP TABLE IF EXISTS xge_stmt").succeeded());
        assertTrue(executor.executeStatement("CREATE TABLE xge_stmt(a INTEGER, b VARCHAR(20))").succeeded());
        assertTrue(executor.executeStatement("INSERT INTO xge_stmt VALUES(1, 'x')").succeeded());

        QueryResult result = executor.executeQuery("SELECT a, b FROM xge_stmt");

        assertTrue(result.succeeded());
        assertEquals(List.of(List.of("1", "x")), result.rows());
    }

    @Test
    void queryReturnsRawValuesAndSqlNullAsNull() {
        executor.executeStatement("DROP TABLE IF EXISTS xge_null");
        executor.executeStatement("CREATE TABLE xge_null(a INTEGER, b VARCHAR(20))");
        executor.executeStatement("INSERT INTO xge_null VALUES(1, NULL)");

        QueryResult result = executor.executeQuery("SELECT a, b FROM xge_null");

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
                () -> executor.executeStatement("CREATE TABLE xge_closed(a INTEGER)"));
    }

    @Test
    void closedConnectionIsFatalForQueries() throws SQLException {
        connection.close();

        assertThrows(FatalDatabaseException.class, () -> executor.executeQuery("SELECT 1"));
    }

    @Test
    void connectionIsNotClosedByTheExecutor() throws SQLException {
        executor.executeStatement("DROP TABLE IF EXISTS xge_keep");
        executor.executeStatement("CREATE TABLE xge_keep(a INTEGER)");
        executor.executeQuery("SELECT a FROM xge_keep");

        assertFalse(connection.isClosed());
    }
}
