package com.ggtest.model;

/**
 * A {@code halt} directive record.
 *
 * @param location source location of the record
 */
public record HaltRecord(SourceLocation location) implements SqlTestRecord {
}
