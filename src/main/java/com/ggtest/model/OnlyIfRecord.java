package com.ggtest.model;

/**
 * An {@code onlyif <db-name>} directive record. Runtime evaluation belongs to the
 * runner slice.
 *
 * @param dbName   the database name operand
 * @param location source location of the record
 */
public record OnlyIfRecord(String dbName, SourceLocation location) implements SqlTestRecord {
}
