package com.ggtest.normalize;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Infers row-wise vs value-per-line expected bodies and expands them to a value
 * sequence for comparison. Package-private helper for {@link ResultComparer}.
 */
final class ExpectedResultExpander {

    private ExpectedResultExpander() {}

    /**
     * Expands expected text to value lines (same granularity as rendered actual values).
     *
     * <p>Single-line hash expectations are returned unchanged (no row-wise expand).
     * Otherwise each physical line is split on the literal {@code columnSeparator}
     * without collapsing consecutive delimiters. When {@code explicitColumnSeparator}
     * is true, each token is trimmed (R2); the trimmed token text is the cell as-is
     * (no quote shell). If every line has exactly {@code typeSignature.size()} tokens,
     * the segment is treated as row-wise and sorted/flattened per {@code sortMode};
     * empty tokens become {@code (empty)} so they align with T-normalized empty cells.
     * Mixed token counts throw {@link IllegalArgumentException}.
     */
    static List<String> expand(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            String columnSeparator,
            boolean explicitColumnSeparator,
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

        int columns = typeSignature.size();
        List<List<String>> tokenRows = new ArrayList<>(physicalLines.size());
        int rowWiseCount = 0;
        for (int i = 0; i < physicalLines.size(); i++) {
            List<String> tokens =
                    tokenize(physicalLines.get(i), columnSeparator, explicitColumnSeparator);
            tokenRows.add(tokens);
            if (tokens.size() == columns) {
                rowWiseCount++;
            }
        }

        if (rowWiseCount == physicalLines.size()) {
            List<List<String>> rows = new ArrayList<>(tokenRows.size());
            for (List<String> tokens : tokenRows) {
                List<String> row = new ArrayList<>(tokens.size());
                for (String token : tokens) {
                    row.add(token.isEmpty() ? "(empty)" : token);
                }
                rows.add(row);
            }
            return ResultSorter.sortAndFlatten(sortMode, rows);
        }

        if (rowWiseCount > 0) {
            StringBuilder detail = new StringBuilder();
            detail.append("mixed expected line shapes for ")
                    .append(columns)
                    .append(" column(s); row-wise requires every line to have ")
                    .append(columns)
                    .append(" token(s) when split on the column separator:");
            for (int i = 0; i < tokenRows.size(); i++) {
                detail.append(" line ")
                        .append(i + 1)
                        .append(" has ")
                        .append(tokenRows.get(i).size())
                        .append(" token(s)");
                if (i + 1 < tokenRows.size()) {
                    detail.append(';');
                }
            }
            throw new IllegalArgumentException(detail.toString());
        }

        return physicalLines;
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

    private static List<String> tokenize(String line, String delim, boolean explicit) {
        List<String> raw = splitLiteral(line, delim);
        if (!explicit) {
            return raw;
        }
        List<String> tokens = new ArrayList<>(raw.size());
        for (String token : raw) {
            tokens.add(token.strip());
        }
        return tokens;
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
