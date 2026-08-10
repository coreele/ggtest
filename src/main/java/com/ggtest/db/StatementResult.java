package com.ggtest.db;

/**
 * Outcome of {@link DatabaseExecutor#executeStatement(String)}.
 *
 * <p>{@code errorSummary} is failure material for reports and may be matched
 * against expected error messages (case-insensitive sub-string) when a
 * {@code statement error} record carries an expected message.
 *
 * @param succeeded    whether the database accepted the statement
 * @param errorSummary short failure description, empty when {@code succeeded}
 */
public record StatementResult(boolean succeeded, String errorSummary) {

    public StatementResult {
        errorSummary = errorSummary == null ? "" : errorSummary;
    }

    /** Returns a successful result. */
    public static StatementResult ok() {
        return new StatementResult(true, "");
    }

    /**
     * Returns a business failure.
     *
     * @param errorSummary short failure description; {@code null} becomes empty
     */
    public static StatementResult failed(String errorSummary) {
        return new StatementResult(false, errorSummary);
    }
}
