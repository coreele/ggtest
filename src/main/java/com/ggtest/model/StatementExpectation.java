package com.ggtest.model;

/**
 * Expected outcome polarity of a {@code statement} record. {@code statement error}
 * may optionally carry an expected error message to match against the database
 * error summary (case-insensitive sub-string).
 */
public enum StatementExpectation {
    OK,
    ERROR
}
