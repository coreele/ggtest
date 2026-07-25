package com.ggtest.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Outcome of {@link DatabaseExecutor#executeQuery(String)}.
 *
 * <p>Rows hold raw values in result-set order; a {@code null} element means SQL
 * NULL. {@code List.copyOf} is therefore unusable here, so rows are copied into
 * unmodifiable lists that tolerate {@code null} elements.
 *
 * @param succeeded    whether the database accepted the query
 * @param rows         raw values, empty on failure
 * @param errorSummary short failure description, empty when {@code succeeded}
 */
public record QueryResult(boolean succeeded, List<List<String>> rows, String errorSummary) {

    public QueryResult {
        rows = copyRows(rows);
        errorSummary = errorSummary == null ? "" : errorSummary;
    }

    /**
     * Returns a successful result.
     *
     * @param rows raw values, {@code null} elements allowed for SQL NULL
     */
    public static QueryResult succeeded(List<List<String>> rows) {
        return new QueryResult(true, rows, "");
    }

    /**
     * Returns a business failure with no rows.
     *
     * @param errorSummary short failure description; {@code null} becomes empty
     */
    public static QueryResult failed(String errorSummary) {
        return new QueryResult(false, List.of(), errorSummary);
    }

    private static List<List<String>> copyRows(List<List<String>> rows) {
        Objects.requireNonNull(rows, "rows");
        List<List<String>> copy = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            Objects.requireNonNull(row, "row");
            copy.add(Collections.unmodifiableList(new ArrayList<>(row)));
        }
        return Collections.unmodifiableList(copy);
    }
}
