package com.ggtest.normalize;

import com.ggtest.model.ColumnType;
import java.util.Locale;

/**
 * Converts a single raw column value to sqllogictest-normalized text for the
 * given {@link ColumnType}.
 *
 * <p>Illegal integer / real inputs follow sqllogictest: non-numeric {@code I}
 * becomes {@code "0"}; unparsable {@code R} becomes {@code "0.000"}. Float-like
 * numeric strings for {@code I} are truncated toward zero (official {@code %d}).
 * This is intentional Spec alignment (CA-008), not error swallowing.
 */
public final class ValueNormalizer {

    private ValueNormalizer() {}

    /**
     * Normalizes {@code raw} according to Spec I/T/R rules.
     *
     * <p>For {@link ColumnType#INTEGER} / {@link ColumnType#REAL}, values that
     * cannot be interpreted as numbers are normalized to {@code "0"} /
     * {@code "0.000"} respectively (sqllogictest-compatible; not a silent bug).
     * Numeric float-like strings for {@code I} truncate toward zero.
     *
     * @param type column type from the query type signature
     * @param raw  raw value; {@code null} means SQL NULL
     * @return normalized single-line text
     */
    public static String normalize(ColumnType type, String raw) {
        if (raw == null) {
            return "NULL";
        }
        return switch (type) {
            case INTEGER -> normalizeInteger(raw);
            case REAL -> normalizeReal(raw);
            case TEXT -> normalizeText(raw);
        };
    }

    /**
     * Interprets {@code raw} as a number and formats as integer text with
     * toward-zero truncation (sqllogictest {@code %d}). Non-numeric input,
     * NaN, and Infinity yield {@code "0"}.
     */
    private static String normalizeInteger(String raw) {
        try {
            double parsed = Double.parseDouble(raw);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return "0";
            }
            long value = (long) parsed;
            return Long.toString(value);
        } catch (NumberFormatException ex) {
            return "0";
        }
    }

    /**
     * Parses {@code raw} as a double and formats to three decimal places; on
     * {@link NumberFormatException} returns {@code "0.000"} (sqllogictest
     * illegal-{@code R} rule).
     */
    private static String normalizeReal(String raw) {
        try {
            double value = Double.parseDouble(raw);
            return String.format(Locale.ROOT, "%.3f", value);
        } catch (NumberFormatException ex) {
            return "0.000";
        }
    }

    private static String normalizeText(String raw) {
        if (raw.isEmpty()) {
            return "(empty)";
        }
        StringBuilder out = new StringBuilder(raw.length());
        raw.codePoints().forEach(cp -> {
            if (cp < 0x20 || cp > 0x7E) {
                out.append('@');
            } else {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }
}
