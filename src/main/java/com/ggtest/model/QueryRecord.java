package com.ggtest.model;

import java.util.List;
import java.util.Optional;

/**
 * A {@code query} record: a SQL query with a column type signature, sort mode,
 * optional label, and optionally an expected result block.
 *
 * <p>When the source omits the expectation header ({@code ----} or
 * {@code ---- separator <delim>}), {@code hasExpectedResults} is {@code false}
 * and {@code expectedResults} is empty ("execute only" semantics). When present,
 * the raw lines after the header up to the record boundary are preserved verbatim
 * in {@code expectedResults}; normalization and hashing belong to the
 * {@code normalize} slice.
 *
 * <p>{@code columnSeparator} / {@code explicitColumnSeparator} are bound from this
 * query's expectation header only (default space, non-explicit when the header is
 * exactly {@code ----}).
 *
 * @param typeSignature             column types, one entry per signature character
 * @param sortMode                  declared sort mode ({@link SortMode#NOSORT} by default)
 * @param label                     optional query label
 * @param sql                       the query SQL text (may span multiple lines, joined by {@code \n})
 * @param hasExpectedResults        whether an expected-result block was present
 * @param expectedResults           raw expected-result lines (empty when absent)
 * @param columnSeparator           column separator for row-wise expected lines
 * @param explicitColumnSeparator   whether the header used {@code ---- separator}
 * @param location                  source location of the record
 */
public record QueryRecord(
        List<ColumnType> typeSignature,
        SortMode sortMode,
        Optional<String> label,
        String sql,
        boolean hasExpectedResults,
        List<String> expectedResults,
        String columnSeparator,
        boolean explicitColumnSeparator,
        SourceLocation location) implements SqlTestRecord {

    public QueryRecord {
        typeSignature = List.copyOf(typeSignature);
        expectedResults = List.copyOf(expectedResults);
        if (columnSeparator == null || columnSeparator.isEmpty()) {
            throw new IllegalArgumentException("columnSeparator must not be empty");
        }
    }
}
