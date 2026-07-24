package com.ggtest.model;

/**
 * A {@code hash-threshold <N>} directive record.
 *
 * @param threshold the threshold operand
 * @param location  source location of the record
 */
public record HashThresholdRecord(int threshold, SourceLocation location) implements SqlTestRecord {
}
