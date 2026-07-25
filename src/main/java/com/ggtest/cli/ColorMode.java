package com.ggtest.cli;

import java.util.Locale;

/**
 * When ANSI colors may appear in the CLI report.
 *
 * <p>Values match {@code --color}, system property {@code ggtest.color}, and
 * environment variable {@code GGTEST_COLOR}.
 */
public enum ColorMode {
    AUTO,
    ALWAYS,
    NEVER;

    static ColorMode parse(String raw, String sourceLabel) {
        if (raw == null || raw.isBlank()) {
            throw new UsageException("invalid " + sourceLabel + " value: (empty)");
        }
        String normalized = raw.strip().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "auto" -> AUTO;
            case "always" -> ALWAYS;
            case "never" -> NEVER;
            default -> throw new UsageException("invalid " + sourceLabel + " value: " + raw
                    + "; allowed: auto, always, never");
        };
    }
}
