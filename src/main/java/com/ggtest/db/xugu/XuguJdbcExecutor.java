package com.ggtest.db.xugu;

import com.ggtest.db.AbstractJdbcExecutor;
import com.ggtest.db.DatabaseExecutor;
import java.sql.Connection;
import java.util.List;

/**
 * {@link DatabaseExecutor} backed by an open XuguDB (虚谷) JDBC connection
 * ({@code com.xugudb:xugu-jdbc}, driver class {@code com.xugu.cloudjdbc.Driver}).
 *
 * <p>The caller owns the connection: this class opens nothing, closes nothing,
 * and never creates or drops schemas. Isolation is orchestrated by the CLI via
 * {@link XuguSchemaIsolation}.
 *
 * <p>Values are returned exactly as {@code getString} yields them, with SQL NULL
 * as {@code null}; I/T/R normalization belongs to {@code com.ggtest.normalize}.
 * Identifiers fold to upper case (Oracle-like) — consistent within a file.
 */
public final class XuguJdbcExecutor extends AbstractJdbcExecutor {

    /** Engine name matched by {@code skipif} / {@code onlyif} operands. */
    public static final String ENGINE_NAME = "xugu";

    // Connection-level fatal markers; Xugu emits localized (Chinese) messages, so
    // both English and Chinese phrases are listed. AbstractJdbcExecutor also
    // classifies via SQLState "08*" and connection.isClosed().
    private static final List<String> FATAL_MESSAGE_MARKERS = List.of(
            "connection closed",
            "connection is closed",
            "connection has been closed",
            "连接已关闭",
            "与服务器间的连接已经断开");

    /**
     * @param connection an already-open XuguDB connection owned by the caller
     */
    public XuguJdbcExecutor(Connection connection) {
        super(connection, FATAL_MESSAGE_MARKERS, "XuguDB");
    }

    @Override
    public String engineName() {
        return ENGINE_NAME;
    }
}
