package com.ggtest.db.postgres;

import com.ggtest.db.DatabaseExecutor;
import com.ggtest.db.FatalDatabaseException;
import com.ggtest.db.QueryResult;
import com.ggtest.db.StatementResult;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * {@link DatabaseExecutor} backed by an open PostgreSQL JDBC connection
 * ({@code org.postgresql:postgresql}).
 *
 * <p>The caller owns the connection: this class opens nothing, closes nothing,
 * and never creates or drops schemas. Isolation is orchestrated by the CLI via
 * {@link PostgresSchemaIsolation}.
 *
 * <p>Values are returned exactly as {@code getString} yields them, with SQL NULL
 * as {@code null}; I/T/R normalization belongs to {@code com.ggtest.normalize}.
 */
public final class PostgresJdbcExecutor implements DatabaseExecutor {

    /** Engine name matched by {@code skipif} / {@code onlyif} operands. */
    public static final String ENGINE_NAME = "postgres";

    private static final List<String> FATAL_MESSAGE_MARKERS = List.of(
            "connection closed",
            "connection is closed",
            "connection has been closed",
            "this connection has been closed",
            "connection has been terminated");

    private final Connection connection;

    /**
     * @param connection an already-open PostgreSQL connection owned by the caller
     */
    public PostgresJdbcExecutor(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }

    @Override
    public StatementResult executeStatement(String sql) {
        requireUsableConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            return StatementResult.ok();
        } catch (SQLException ex) {
            throwIfFatal(ex);
            return StatementResult.failed(summarize(ex));
        }
    }

    @Override
    public QueryResult executeQuery(String sql) {
        requireUsableConnection();
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            return QueryResult.succeeded(readRows(resultSet));
        } catch (SQLException ex) {
            throwIfFatal(ex);
            return QueryResult.failed(summarize(ex));
        }
    }

    private static List<List<String>> readRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columns = metaData.getColumnCount();
        List<List<String>> rows = new ArrayList<>();
        while (resultSet.next()) {
            List<String> row = new ArrayList<>(columns);
            for (int column = 1; column <= columns; column++) {
                String value = resultSet.getString(column);
                row.add(resultSet.wasNull() ? null : value);
            }
            rows.add(row);
        }
        return rows;
    }

    private void requireUsableConnection() {
        if (isConnectionUnusable()) {
            throw new FatalDatabaseException("PostgreSQL connection is not usable");
        }
    }

    private void throwIfFatal(SQLException ex) {
        if (isFatal(ex)) {
            throw new FatalDatabaseException(describeFatal(ex), ex);
        }
    }

    private boolean isFatal(SQLException ex) {
        String sqlState = ex.getSQLState();
        if (sqlState != null && sqlState.startsWith("08")) {
            return true;
        }
        if (isConnectionUnusable()) {
            return true;
        }
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
        for (String marker : FATAL_MESSAGE_MARKERS) {
            if (message.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private boolean isConnectionUnusable() {
        try {
            return connection.isClosed();
        } catch (SQLException ex) {
            return true;
        }
    }

    private static String describeFatal(SQLException ex) {
        return "PostgreSQL connection failure: " + summarize(ex);
    }

    /** Failure material for reports; carries no connection string or credentials. */
    private static String summarize(SQLException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message.strip();
    }
}
