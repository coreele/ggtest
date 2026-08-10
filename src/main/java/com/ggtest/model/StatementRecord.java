package com.ggtest.model;

/**
 * A {@code statement ok} / {@code statement error} record: a SQL statement plus
 * its expected success/failure polarity and an optional expected error message.
 *
 * @param sql              the statement SQL text (may span multiple lines, joined by {@code \n})
 * @param expectation      whether the statement is expected to succeed or fail
 * @param expectedErrorMsg optional expected error message for {@code statement error};
 *                         {@code null} when absent or for {@code statement ok}
 * @param location         source location of the record
 */
public record StatementRecord(
        String sql, StatementExpectation expectation, String expectedErrorMsg, SourceLocation location)
        implements SqlTestRecord {
}
