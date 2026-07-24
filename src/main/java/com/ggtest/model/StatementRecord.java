package com.ggtest.model;

/**
 * A {@code statement ok} / {@code statement error} record: a SQL statement plus
 * its expected success/failure polarity.
 *
 * @param sql         the statement SQL text (may span multiple lines, joined by {@code \n})
 * @param expectation whether the statement is expected to succeed or fail
 * @param location    source location of the record
 */
public record StatementRecord(String sql, StatementExpectation expectation, SourceLocation location)
        implements SqlTestRecord {
}
