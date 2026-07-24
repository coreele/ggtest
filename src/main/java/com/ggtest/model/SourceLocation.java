package com.ggtest.model;

/**
 * Identifies where a record originates in the source input.
 *
 * @param sourceName logical name of the source (usually a file path); used for error and report location
 * @param startLine  1-based line number of the record's first line
 */
public record SourceLocation(String sourceName, int startLine) {
}
