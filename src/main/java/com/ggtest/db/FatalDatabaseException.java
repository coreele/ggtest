package com.ggtest.db;

/**
 * Signals that the database connection is unusable — it could not be
 * established, was closed, or was interrupted mid-execution.
 *
 * <p>The runner aborts the current file when this is thrown, keeping the results
 * produced so far. Ordinary SQL errors are business failures instead and are
 * reported through {@link StatementResult} / {@link QueryResult}.
 *
 * <p>Messages must not carry credentials or connection secrets.
 */
public class FatalDatabaseException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public FatalDatabaseException(String message) {
        super(message);
    }

    public FatalDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
