package com.ggtest.normalize;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compares expected sqllogictest result text against actual raw query rows
 * after I/T/R normalization, sorting, and optional MD5 hashing.
 *
 * <p>Supports value-per-line expectations (default) and row-wise expectations
 * inferred from column count and the current query's column separator. When the
 * expectation header used {@code ---- separator}, tokens are trimmed after split;
 * there is no quote shell on expected cells.
 *
 * <p>No JDBC dependency — callers supply already-fetched values.
 */
public final class ResultComparer {

    /** Default hash-threshold initial value (Q5). */
    public static final int DEFAULT_HASH_THRESHOLD = 8;

    /** Default column separator for row-wise expected lines (U+0020). */
    public static final String DEFAULT_COLUMN_SEPARATOR = " ";

    private ResultComparer() {}

    /**
     * Outcome of a result comparison.
     *
     * @param passed       whether actual matches expected
     * @param expectedView expected side as compared (full lines or hash form)
     * @param actualView   actual side as compared
     * @param diffSummary  non-empty when {@code passed} is false; git-style material for failure reports
     */
    public record CompareResult(
            boolean passed,
            List<String> expectedView,
            List<String> actualView,
            String diffSummary) {}

    /**
     * Compares expected text to actual rows using {@link #DEFAULT_COLUMN_SEPARATOR}
     * and a non-explicit separator (default space row-wise rules).
     *
     * @param typeSignature  column types
     * @param sortMode       sort mode applied before compare/hash
     * @param hashThreshold  current threshold; {@code <= 0} disables hashing
     * @param expectedText   expected body after the expectation header
     * @param actualRows     raw result rows
     */
    public static CompareResult compare(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            int hashThreshold,
            String expectedText,
            List<List<String>> actualRows) {
        return compare(
                typeSignature,
                sortMode,
                hashThreshold,
                DEFAULT_COLUMN_SEPARATOR,
                false,
                expectedText,
                actualRows);
    }

    /**
     * Compares expected text to actual rows with an explicit/non-explicit column separator.
     *
     * @param typeSignature              column types
     * @param sortMode                   sort mode applied before compare/hash
     * @param hashThreshold              current threshold; {@code <= 0} disables hashing
     * @param columnSeparator            literal separator for inferring/splitting row-wise lines
     * @param explicitColumnSeparator    whether the query expectation header used {@code ---- separator}
     * @param expectedText               expected body after the expectation header
     * @param actualRows                 raw result rows
     */
    public static CompareResult compare(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            int hashThreshold,
            String columnSeparator,
            boolean explicitColumnSeparator,
            String expectedText,
            List<List<String>> actualRows) {
        Objects.requireNonNull(typeSignature, "typeSignature");
        Objects.requireNonNull(sortMode, "sortMode");
        Objects.requireNonNull(columnSeparator, "columnSeparator");
        Objects.requireNonNull(actualRows, "actualRows");
        if (typeSignature.isEmpty()) {
            throw new IllegalArgumentException("typeSignature must not be empty");
        }
        if (columnSeparator.isEmpty()) {
            throw new IllegalArgumentException("columnSeparator must not be empty");
        }

        List<String> normalized = ResultSorter.normalizeAndSort(typeSignature, sortMode, actualRows);
        List<String> expectedLines = ExpectedResultExpander.expand(
                typeSignature, sortMode, columnSeparator, explicitColumnSeparator, expectedText);
        List<String> actualLines = renderActual(normalized, hashThreshold);

        if (expectedLines.equals(actualLines)) {
            return new CompareResult(true, expectedLines, actualLines, "");
        }
        return new CompareResult(false, expectedLines, actualLines, buildDiffSummary(expectedLines, actualLines));
    }

    /**
     * Convenience overload using {@link #DEFAULT_HASH_THRESHOLD} and
     * {@link #DEFAULT_COLUMN_SEPARATOR} (non-explicit).
     */
    public static CompareResult compare(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            String expectedText,
            List<List<String>> actualRows) {
        return compare(
                typeSignature,
                sortMode,
                DEFAULT_HASH_THRESHOLD,
                DEFAULT_COLUMN_SEPARATOR,
                false,
                expectedText,
                actualRows);
    }

    static List<String> renderActual(List<String> normalizedValues, int hashThreshold) {
        if (hashThreshold > 0 && normalizedValues.size() > hashThreshold) {
            return List.of(ResultHasher.hashForm(normalizedValues));
        }
        return List.copyOf(normalizedValues);
    }

    /** Retained for package tests that inspect physical expected lines. */
    static List<String> splitExpectedLines(String expectedText) {
        return ExpectedResultExpander.physicalLines(expectedText);
    }

    /**
     * Git-style line diff: unchanged lines prefixed with four spaces; expected-only
     * with {@code -   }; actual-only with {@code +   }. Comparison semantics are
     * unchanged — this only affects presentation.
     */
    static String buildDiffSummary(List<String> expected, List<String> actual) {
        List<DiffOp> ops = diffOps(expected, actual);
        StringBuilder sb = new StringBuilder();
        for (DiffOp op : ops) {
            switch (op.kind()) {
                case EQUAL -> sb.append("    ").append(op.line()).append('\n');
                case DELETE -> sb.append("-   ").append(op.line()).append('\n');
                case INSERT -> sb.append("+   ").append(op.line()).append('\n');
            }
        }
        return sb.toString();
    }

    private static List<DiffOp> diffOps(List<String> expected, List<String> actual) {
        int n = expected.size();
        int m = actual.size();
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (expected.get(i).equals(actual.get(j))) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        List<DiffOp> ops = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (expected.get(i).equals(actual.get(j))) {
                ops.add(new DiffOp(DiffKind.EQUAL, expected.get(i)));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                ops.add(new DiffOp(DiffKind.DELETE, expected.get(i)));
                i++;
            } else {
                ops.add(new DiffOp(DiffKind.INSERT, actual.get(j)));
                j++;
            }
        }
        while (i < n) {
            ops.add(new DiffOp(DiffKind.DELETE, expected.get(i++)));
        }
        while (j < m) {
            ops.add(new DiffOp(DiffKind.INSERT, actual.get(j++)));
        }
        return ops;
    }

    private enum DiffKind {
        EQUAL,
        DELETE,
        INSERT
    }

    private record DiffOp(DiffKind kind, String line) {}
}
