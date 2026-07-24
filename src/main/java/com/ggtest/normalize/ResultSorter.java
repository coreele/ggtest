package com.ggtest.normalize;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Expands raw result rows into normalized values and applies {@link SortMode}
 * before comparison or hashing.
 */
public final class ResultSorter {

    private ResultSorter() {}

    /**
     * Normalizes each cell, then sorts according to {@code sortMode}.
     *
     * @param typeSignature column types (length = columns per row)
     * @param sortMode      nosort / rowsort / valuesort
     * @param rows          raw rows; each inner list is one row of column values
     * @return flattened normalized values in comparison order
     */
    public static List<String> normalizeAndSort(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            List<List<String>> rows) {
        int columns = typeSignature.size();
        List<List<String>> normalizedRows = new ArrayList<>(rows.size());
        for (List<String> row : rows) {
            if (row.size() != columns) {
                throw new IllegalArgumentException(
                        "row width " + row.size() + " != type signature length " + columns);
            }
            List<String> normalized = new ArrayList<>(columns);
            for (int i = 0; i < columns; i++) {
                normalized.add(ValueNormalizer.normalize(typeSignature.get(i), row.get(i)));
            }
            normalizedRows.add(normalized);
        }

        return switch (sortMode) {
            case NOSORT -> flatten(normalizedRows);
            case ROWSORT -> {
                List<List<String>> sorted = new ArrayList<>(normalizedRows);
                sorted.sort(rowComparator());
                yield flatten(sorted);
            }
            case VALUESORT -> {
                List<String> values = flatten(normalizedRows);
                values.sort(Comparator.naturalOrder());
                yield values;
            }
        };
    }

    private static Comparator<List<String>> rowComparator() {
        return (a, b) -> {
            int n = Math.min(a.size(), b.size());
            for (int i = 0; i < n; i++) {
                int c = a.get(i).compareTo(b.get(i));
                if (c != 0) {
                    return c;
                }
            }
            return Integer.compare(a.size(), b.size());
        };
    }

    private static List<String> flatten(List<List<String>> rows) {
        List<String> out = new ArrayList<>();
        for (List<String> row : rows) {
            out.addAll(row);
        }
        return out;
    }
}
