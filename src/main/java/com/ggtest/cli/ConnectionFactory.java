package com.ggtest.cli;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Creates JDBC connections from parsed CLI options. Package-private. */
final class ConnectionFactory {

    private ConnectionFactory() {}

    static Connection open(CliOptions options) throws SQLException {
        Properties properties = new Properties();
        options.user().ifPresent(user -> properties.setProperty("user", user));
        options.password().ifPresent(password -> properties.setProperty("password", password));
        try {
            if (properties.isEmpty()) {
                return DriverManager.getConnection(options.url());
            }
            return DriverManager.getConnection(options.url(), properties);
        } catch (RuntimeException ex) {
            throw new SQLException("JDBC driver connection error: " + summarize(ex), ex);
        }
    }

    private static String summarize(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message.strip();
    }
}
