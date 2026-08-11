package com.ggtest.db;

/**
 * Database-side extension point used by the runner: reports the engine name and
 * runs statements and queries against an already-open connection.
 *
 * <p>Implementations live in engine-specific sub-packages (for example
 * {@code com.ggtest.db.sqlite}). Adding support for another database means adding
 * an implementation only — the parser and runner stay unchanged.
 *
 * <p>Failure split: a SQL statement rejected by the database is a <em>business</em>
 * failure and is reported through the returned result object, so the runner can
 * keep executing the remaining records. A broken or closed connection is
 * <em>fatal</em> and must be signalled with {@link FatalDatabaseException}, which
 * aborts the current file.
 *
 * <p>Connection ownership stays with the caller: implementations neither open,
 * initialize, nor close connections.
 */
public interface DatabaseExecutor {

    /**
     * Returns the database name matched against {@code skipif} / {@code onlyif}
     * operands (case-insensitively by the runner), for example {@code "sqlite"}.
     */
    String engineName();

    /**
     * Executes a statement whose result set, if any, is ignored.
     *
     * @param sql statement text
     * @return success, or a business failure with an optional error summary
     * @throws FatalDatabaseException when the connection is unusable
     */
    StatementResult executeStatement(String sql);

    /**
     * Executes a query and returns its raw column values, row by row.
     *
     * <p>Values are returned unnormalized: a {@code null} element means SQL NULL.
     * I/T/R normalization, sorting, and hashing belong to {@code com.ggtest.normalize}.
     *
     * @param sql query text
     * @return rows on success, or a business failure with an optional error summary
     * @throws FatalDatabaseException when the connection is unusable
     */
    QueryResult executeQuery(String sql);

    /**
     * Executes a statement with a maximum execution time.
     *
     * @param sql       statement text
     * @param timeoutMs timeout in milliseconds; 0 means no timeout
     * @return success, or a business failure with an optional error summary
     * @throws FatalDatabaseException when the connection is unusable
     */
    default StatementResult executeStatement(String sql, int timeoutMs) {
        return executeStatement(sql);
    }

    /**
     * Executes a query with a maximum execution time.
     *
     * @param sql       query text
     * @param timeoutMs timeout in milliseconds; 0 means no timeout
     * @return rows on success, or a business failure with an optional error summary
     * @throws FatalDatabaseException when the connection is unusable
     */
    default QueryResult executeQuery(String sql, int timeoutMs) {
        return executeQuery(sql);
    }
}
