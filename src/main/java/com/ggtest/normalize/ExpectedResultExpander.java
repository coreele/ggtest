package com.ggtest.normalize;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Expands expected result bodies to a value sequence for comparison.
 * Package-private helper for {@link ResultComparer}.
 */
final class ExpectedResultExpander {

    private ExpectedResultExpander() {}

    /**
     * Expands expected text to value lines (same granularity as rendered actual values).
     *
     * <p>Single-line hash expectations are returned unchanged. Without a declared
     * {@code columnSeparator}, each physical line is one cell (value-per-line).
     * With a declared separator, each physical line is split on the literal delimiter
     * (no collapsing of consecutive delimiters); tokens are trimmed; empty tokens become
     * {@code (empty)}; each line must have exactly {@code typeSignature.size()} tokens
     * or an {@link IllegalArgumentException} describes the mismatch. Matching rows are
     * sorted/flattened per {@code sortMode}.
     */
    static List<String> expand(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            Optional<String> columnSeparator,
            String expectedText) {
        List<String> physicalLines = physicalLines(expectedText);
        Optional<ResultHasher.HashExpectation> hash =
                ResultHasher.parseHashExpectation(String.join("\n", physicalLines));
        if (hash.isPresent() && physicalLines.size() == 1) {
            return physicalLines;
        }
        if (physicalLines.isEmpty()) {
            return List.of();
        }

        if (columnSeparator.isEmpty()) {
            return physicalLines;
        }

        String delim = columnSeparator.get();
        int columns = typeSignature.size();
        List<List<String>> rows = new ArrayList<>(physicalLines.size());
        for (int i = 0; i < physicalLines.size(); i++) {
            List<String> raw = splitLiteral(physicalLines.get(i), delim);
            List<String> tokens = new ArrayList<>(raw.size());
            for (String token : raw) {
                tokens.add(token.strip());
            }
            if (tokens.size() != columns) {
                throw new IllegalArgumentException(
                        "row-wise expected line "
                                + (i + 1)
                                + " has "
                                + tokens.size()
                                + " token(s) but type signature requires "
                                + columns
                                + " column(s)");
            }
            List<String> row = new ArrayList<>(tokens.size());
            for (String token : tokens) {
                row.add(token.isEmpty() ? "(empty)" : token);
            }
            rows.add(row);
        }
        return ResultSorter.sortAndFlatten(sortMode, rows);
    }

    static List<String> physicalLines(String expectedText) {
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
        return lines;
    }

    /** Splits {@code line} on literal {@code delim} without collapsing repeats. */
    static List<String> splitLiteral(String line, String delim) {
        if (delim == null || delim.isEmpty()) {
            throw new IllegalArgumentException("column separator must not be empty");
        }
        List<String> tokens = new ArrayList<>();
        int start = 0;
        while (true) {
            int idx = line.indexOf(delim, start);
            if (idx < 0) {
                tokens.add(line.substring(start));
                break;
            }
            tokens.add(line.substring(start, idx));
            start = idx + delim.length();
        }
        return tokens;
    }
}
