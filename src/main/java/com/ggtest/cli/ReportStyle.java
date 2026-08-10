package com.ggtest.cli;

/**
 * Package-private ANSI helpers for report structure tags only.
 * Layout and plain text stay identical with color on or off.
 */
final class ReportStyle {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private final boolean ansi;

    ReportStyle(boolean ansi) {
        this.ansi = ansi;
    }

    boolean ansiEnabled() {
        return ansi;
    }

    String passedTag() {
        return color(GREEN, "[PASSED]");
    }

    String failedTag() {
        return color(RED, "[FAILED]");
    }

    String skippedTag() {
        return color(YELLOW, "[SKIPPED]");
    }

    String overriddenTag() {
        return color(CYAN, "[OVERRIDDEN]");
    }

    String label(String bracketLabel) {
        return color(CYAN, bracketLabel);
    }

    String diffMinus(String line) {
        return color(RED, line);
    }

    String diffPlus(String line) {
        return color(GREEN, line);
    }

    private String color(String code, String text) {
        if (!ansi) {
            return text;
        }
        return code + text + RESET;
    }
}
