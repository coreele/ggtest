package com.ggtest.db;

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
 * Shared JDBC execution logic for engine-specific {@link DatabaseExecutor}
 * implementations. Subclasses supply engine markers and display names; this
 * class handles statement/query execution, row reading, and fatal-vs-business
 * failure classification.
 */
public abstract class AbstractJdbcExecutor implements DatabaseExecutor {

    private final Connection connection;
    private final List<String> fatalMessageMarkers;
    private final String engineDisplayName;

    protected AbstractJdbcExecutor(
            Connection connection, List<String> fatalMessageMarkers, String engineDisplayName) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.fatalMessageMarkers = List.copyOf(fatalMessageMarkers);
        this.engineDisplayName = Objects.requireNonNull(engineDisplayName, "engineDisplayName");
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
            throw new FatalDatabaseException(engineDisplayName + " connection is not usable");
        }
    }

    /**
     * Rethrows connection-level problems as fatal so the runner aborts the file;
     * ordinary SQL errors fall through and become business failures.
     */
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
        for (String marker : fatalMessageMarkers) {
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

    private String describeFatal(SQLException ex) {
        return engineDisplayName + " connection failure: " + summarize(ex);
    }

    /** Failure material for reports; carries no connection string or credentials. */
    private static String summarize(SQLException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message.strip();
    }
}
