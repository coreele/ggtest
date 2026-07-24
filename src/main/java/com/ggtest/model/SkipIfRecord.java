package com.ggtest.model;

/**
 * A {@code skipif <db-name>} directive record. Runtime evaluation (whether the
 * following record is skipped for a given database) belongs to the runner slice.
 *
 * @param dbName   the database name operand
 * @param location source location of the record
 */
public record SkipIfRecord(String dbName, SourceLocation location) implements SqlTestRecord {
}
