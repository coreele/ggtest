package com.ggtest.model;

/**
 * A {@code statement ok} / {@code statement error} record: a SQL statement plus
 * its expected success/failure polarity, optional expected error message,
 * optional connection name, and optional timeout.
 *
 * @param sql              the statement SQL text (may span multiple lines, joined by {@code \n})
 * @param expectation      whether the statement is expected to succeed or fail
 * @param expectedErrorMsg optional expected error message for {@code statement error};
 *                         {@code null} when absent or for {@code statement ok}
 * @param location         source location of the record
 * @param errorMsgStartColumn 0-based column in the de-CR header line where the message fragment
 *                            after {@code error} begins; {@code -1} when no message is present
 * @param timeoutMs        execution timeout in milliseconds; 0 means no timeout
 * @param conn             named connection identifier; {@code null} means default connection
 */
public record StatementRecord(
        String sql, StatementExpectation expectation, String expectedErrorMsg, SourceLocation location,
        int errorMsgStartColumn, int timeoutMs, String conn) implements SqlTestRecord {

    public StatementRecord(
            String sql, StatementExpectation expectation, String expectedErrorMsg, SourceLocation location) {
        this(sql, expectation, expectedErrorMsg, location, -1, 0, null);
    }

    public StatementRecord(
            String sql, StatementExpectation expectation, String expectedErrorMsg, SourceLocation location,
            int errorMsgStartColumn) {
        this(sql, expectation, expectedErrorMsg, location, errorMsgStartColumn, 0, null);
    }

    public StatementRecord(
            String sql, StatementExpectation expectation, String expectedErrorMsg, SourceLocation location,
            int errorMsgStartColumn, int timeoutMs) {
        this(sql, expectation, expectedErrorMsg, location, errorMsgStartColumn, timeoutMs, null);
    }
}
