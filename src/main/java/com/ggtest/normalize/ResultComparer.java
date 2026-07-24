package com.ggtest.normalize;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Compares expected sqllogictest result text against actual raw query rows
 * after I/T/R normalization, sorting, and optional MD5 hashing.
 *
 * <p>No JDBC dependency — callers supply already-fetched values.
 */
public final class ResultComparer {

    /** Default hash-threshold initial value (Q5). */
    public static final int DEFAULT_HASH_THRESHOLD = 8;

    private ResultComparer() {}

    /**
     * Outcome of a result comparison.
     *
     * @param passed       whether actual matches expected
     * @param expectedView expected side as compared (full lines or hash form)
     * @param actualView   actual side as compared
     * @param diffSummary  non-empty when {@code passed} is false; material for failure reports
     */
    public record CompareResult(
            boolean passed,
            List<String> expectedView,
            List<String> actualView,
            String diffSummary) {}

    /**
     * Compares expected text to actual rows.
     *
     * @param typeSignature  column types
     * @param sortMode       sort mode applied before compare/hash
     * @param hashThreshold  current threshold; {@code <= 0} disables hashing
     * @param expectedText   expected body after {@code ----} (values or hash line)
     * @param actualRows     raw result rows
     */
    public static CompareResult compare(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            int hashThreshold,
            String expectedText,
            List<List<String>> actualRows) {
        Objects.requireNonNull(typeSignature, "typeSignature");
        Objects.requireNonNull(sortMode, "sortMode");
        Objects.requireNonNull(actualRows, "actualRows");
        if (typeSignature.isEmpty()) {
            throw new IllegalArgumentException("typeSignature must not be empty");
        }

        List<String> normalized = ResultSorter.normalizeAndSort(typeSignature, sortMode, actualRows);
        List<String> expectedLines = splitExpectedLines(expectedText);
        List<String> actualLines = renderActual(normalized, hashThreshold);

        if (expectedLines.equals(actualLines)) {
            return new CompareResult(true, expectedLines, actualLines, "");
        }
        return new CompareResult(false, expectedLines, actualLines, buildDiffSummary(expectedLines, actualLines));
    }

    /**
     * Convenience overload using {@link #DEFAULT_HASH_THRESHOLD}.
     */
    public static CompareResult compare(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            String expectedText,
            List<List<String>> actualRows) {
        return compare(typeSignature, sortMode, DEFAULT_HASH_THRESHOLD, expectedText, actualRows);
    }

    static List<String> renderActual(List<String> normalizedValues, int hashThreshold) {
        if (hashThreshold > 0 && normalizedValues.size() > hashThreshold) {
            return List.of(ResultHasher.hashForm(normalizedValues));
        }
        return List.copyOf(normalizedValues);
    }

    static List<String> splitExpectedLines(String expectedText) {
        if (expectedText == null || expectedText.isEmpty()) {
            return List.of();
        }
        String normalized = expectedText.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.endsWith("\n")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            return List.of();
        }
        String[] parts = normalized.split("\n", -1);
        List<String> lines = new ArrayList<>(parts.length);
        for (String part : parts) {
            lines.add(part);
        }
        Optional<ResultHasher.HashExpectation> hash =
                ResultHasher.parseHashExpectation(String.join("\n", lines));
        if (hash.isPresent() && lines.size() == 1) {
            return lines;
        }
        return lines;
    }

    private static String buildDiffSummary(List<String> expected, List<String> actual) {
        StringBuilder sb = new StringBuilder();
        sb.append("expected (").append(expected.size()).append(" lines):\n");
        appendPreview(sb, expected);
        sb.append("actual (").append(actual.size()).append(" lines):\n");
        appendPreview(sb, actual);
        int limit = Math.min(expected.size(), actual.size());
        for (int i = 0; i < limit; i++) {
            if (!expected.get(i).equals(actual.get(i))) {
                sb.append("first difference at line ")
                        .append(i + 1)
                        .append(": expected=")
                        .append(expected.get(i))
                        .append(" actual=")
                        .append(actual.get(i))
                        .append('\n');
                break;
            }
        }
        if (expected.size() != actual.size()) {
            boolean commonPrefixEqual = limit == 0
                    || expected.subList(0, limit).equals(actual.subList(0, limit));
            if (commonPrefixEqual) {
                sb.append("length mismatch: expected ")
                        .append(expected.size())
                        .append(" vs actual ")
                        .append(actual.size())
                        .append('\n');
            }
        }
        return sb.toString();
    }

    private static void appendPreview(StringBuilder sb, List<String> lines) {
        int preview = Math.min(lines.size(), 8);
        for (int i = 0; i < preview; i++) {
            sb.append("  ").append(lines.get(i)).append('\n');
        }
        if (lines.size() > preview) {
            sb.append("  ... (").append(lines.size() - preview).append(" more)\n");
        }
    }
}
