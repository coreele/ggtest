package com.ggtest.model;

import java.util.List;
import java.util.Optional;

/**
 * A {@code query} record: a SQL query with a column type signature, sort mode,
 * optional label, optional connection name, optional timeout, and optionally
 * an expected result block.
 *
 * <p>When the source omits the expectation header ({@code ----}),
 * {@code hasExpectedResults} is {@code false} and {@code expectedResults} is empty
 * ("execute only" semantics). When present, the raw lines after the header up to
 * the record boundary are preserved verbatim in {@code expectedResults};
 * normalization and hashing belong to the {@code normalize} slice.
 *
 * <p>{@code columnSeparator} is set only when the query header declares
 * {@code separator=<delim>}; empty means value-per-line expectations.
 *
 * @param typeSignature      column types, one entry per signature character
 * @param sortMode           declared sort mode ({@link SortMode#NOSORT} by default)
 * @param label              optional query label
 * @param sql                the query SQL text (may span multiple lines, joined by {@code \n})
 * @param hasExpectedResults whether an expected-result block was present
 * @param expectedResults    raw expected-result lines (empty when absent)
 * @param columnSeparator    explicit row-wise delimiter when declared; empty = value-per-line
 * @param location           source location of the record
 * @param expectedHeaderLine 1-based line number of the {@code ----} header; {@code 0} when absent
 * @param expectedBodyEndLine 1-based line number of the last expected-result line;
 *                            equal to {@code expectedHeaderLine} when the block is empty
 * @param timeoutMs          execution timeout in milliseconds; 0 means no timeout
 * @param conn               named connection identifier; {@code null} means default connection
 */
public record QueryRecord(
        List<ColumnType> typeSignature,
        SortMode sortMode,
        Optional<String> label,
        String sql,
        boolean hasExpectedResults,
        List<String> expectedResults,
        Optional<String> columnSeparator,
        SourceLocation location,
        int expectedHeaderLine,
        int expectedBodyEndLine,
        int timeoutMs,
        String conn) implements SqlTestRecord {

    public QueryRecord {
        typeSignature = List.copyOf(typeSignature);
        expectedResults = List.copyOf(expectedResults);
        columnSeparator = columnSeparator == null ? Optional.empty() : columnSeparator;
        if (columnSeparator.isPresent()) {
            String delim = columnSeparator.get();
            if (delim.isEmpty()) {
                throw new IllegalArgumentException("columnSeparator must not be empty when present");
            }
            if (delim.chars().anyMatch(Character::isWhitespace)) {
                throw new IllegalArgumentException("columnSeparator must not contain whitespace");
            }
        }
    }

    public QueryRecord(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            Optional<String> label,
            String sql,
            boolean hasExpectedResults,
            List<String> expectedResults,
            Optional<String> columnSeparator,
            SourceLocation location) {
        this(typeSignature, sortMode, label, sql, hasExpectedResults, expectedResults, columnSeparator,
                location, 0, 0, 0, null);
    }

    public QueryRecord(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            Optional<String> label,
            String sql,
            boolean hasExpectedResults,
            List<String> expectedResults,
            Optional<String> columnSeparator,
            SourceLocation location,
            int expectedHeaderLine,
            int expectedBodyEndLine) {
        this(typeSignature, sortMode, label, sql, hasExpectedResults, expectedResults, columnSeparator,
                location, expectedHeaderLine, expectedBodyEndLine, 0, null);
    }

    public QueryRecord(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            Optional<String> label,
            String sql,
            boolean hasExpectedResults,
            List<String> expectedResults,
            Optional<String> columnSeparator,
            SourceLocation location,
            int expectedHeaderLine,
            int expectedBodyEndLine,
            int timeoutMs) {
        this(typeSignature, sortMode, label, sql, hasExpectedResults, expectedResults, columnSeparator,
                location, expectedHeaderLine, expectedBodyEndLine, timeoutMs, null);
    }
}
