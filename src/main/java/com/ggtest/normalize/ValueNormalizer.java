package com.ggtest.normalize;

import com.ggtest.model.ColumnType;
import java.util.Locale;

/**
 * Converts a single raw column value to sqllogictest-normalized text for the
 * given {@link ColumnType}.
 */
public final class ValueNormalizer {

    private ValueNormalizer() {}

    /**
     * Normalizes {@code raw} according to Spec I/T/R rules.
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

    private static String normalizeInteger(String raw) {
        try {
            long value = Long.parseLong(raw);
            return Long.toString(value);
        } catch (NumberFormatException ex) {
            return "0";
        }
    }

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
