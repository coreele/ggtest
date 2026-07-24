package com.ggtest.model;

/**
 * Expected outcome polarity of a {@code statement} record. {@code statement error}
 * does not carry error-message matching (out of scope for the first iteration).
 */
public enum StatementExpectation {
    OK,
    ERROR
}
