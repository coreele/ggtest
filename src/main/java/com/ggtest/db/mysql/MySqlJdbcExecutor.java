package com.ggtest.db.mysql;

import com.ggtest.db.AbstractJdbcExecutor;
import com.ggtest.db.DatabaseExecutor;
import java.sql.Connection;
import java.util.List;

/**
 * {@link DatabaseExecutor} backed by an open MySQL JDBC connection
 * ({@code com.mysql:mysql-connector-j}, driver class {@code com.mysql.cj.jdbc.Driver}).
 */
public final class MySqlJdbcExecutor extends AbstractJdbcExecutor {

    public static final String ENGINE_NAME = "mysql";

    private static final List<String> FATAL_MESSAGE_MARKERS = List.of(
            "connection closed",
            "connection is closed",
            "connection has been closed",
            "communications link failure",
            "no operations allowed after connection closed");

    public MySqlJdbcExecutor(Connection connection) {
        super(connection, FATAL_MESSAGE_MARKERS, "MySQL");
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }
}
