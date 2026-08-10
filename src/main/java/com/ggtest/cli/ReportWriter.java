package com.ggtest.cli;

import com.ggtest.runner.RecordResult;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Formats and prints CLI report lines (status, failure blocks, summary). */
final class ReportWriter {

    private static final Path CWD = Path.of("").toAbsolutePath().normalize();

    private final PrintStream out;
    private final ReportStyle style;

    ReportWriter(PrintStream out, ReportStyle style) {
        this.out = Objects.requireNonNull(out, "out");
        this.style = Objects.requireNonNull(style, "style");
    }

    void printStatusLine(String path, int pathWidth, String tag, long elapsedMs, boolean withTiming) {
        String padded = String.format("%-" + Math.max(pathWidth, 1) + "s", path);
        if (withTiming) {
            out.printf("%s .. %s in %d ms%n", padded, tag, elapsedMs);
        } else {
            out.printf("%s .. %s%n", padded, tag);
        }
    }

    void printErrorSection(List<String> failedPaths, FileBucket lastBucket) {
        if (failedPaths.isEmpty()) {
            return;
        }
        if (lastBucket != FileBucket.FAILED) {
            out.println();
        }
        out.println("Error: some test case failed:");
        out.println("[");
        for (String path : failedPaths) {
            out.println("    \"" + path + "\",");
        }
        out.println("]");
        out.println();
    }

    void printTrailingBlankIfNeeded(List<String> failedPaths, int totalPassed, int totalSkipped) {
        if (failedPaths.isEmpty() && totalPassed + totalSkipped > 0) {
            out.println();
        }
    }

    void printTotal(int totalPassed, int totalFailed, int totalSkipped) {
        printTotal(totalPassed, totalFailed, totalSkipped, 0, false);
    }

    void printTotal(int totalPassed, int totalFailed, int totalSkipped, int totalOverridden, boolean showOverridden) {
        if (showOverridden) {
            out.printf("TOTAL: passed=%d failed=%d skipped=%d overridden=%d%n",
                    totalPassed, totalFailed, totalSkipped, totalOverridden);
        } else {
            out.printf("TOTAL: passed=%d failed=%d skipped=%d%n", totalPassed, totalFailed, totalSkipped);
        }
    }

    List<String> formatFailureDetailLines(String file, RecordResult recordResult) {
        String reason = recordResult.failureReason() == null ? "" : recordResult.failureReason();
        String why;
        String diffBody = null;
        if (reason.startsWith("result mismatch:")) {
            why = "query result mismatch";
            String remainder = reason.substring("result mismatch:".length());
            if (remainder.startsWith("\n")) {
                remainder = remainder.substring(1);
            }
            if (!remainder.isBlank()) {
                diffBody = remainder;
            }
        } else {
            why = firstLine(reason);
            String rest = afterFirstLine(reason);
            if (!rest.isBlank() && looksLikeGitDiff(rest)) {
                diffBody = rest;
            }
        }
        return detailLines(why, diffBody, file, recordResult.location().startLine());
    }

    List<String> detailLines(String why, String diffBody, String file, Integer line) {
        List<String> lines = new ArrayList<>();
        if (line != null) {
            lines.add("    at " + file + ":" + line + " : " + why);
        } else {
            lines.add("    at " + file + " : " + why);
        }
        if (diffBody != null && !diffBody.isBlank()) {
            lines.add("        (-expected|+actual)");
            for (String raw : diffBody.split("\\R", -1)) {
                if (raw.isEmpty()) {
                    continue;
                }
                lines.add("        " + colorDiffLine(raw));
            }
        }
        return lines;
    }

    static String relativePath(Path file) {
        Path absolute = file.toAbsolutePath().normalize();
        try {
            if (absolute.startsWith(CWD)) {
                Path relative = CWD.relativize(absolute);
                String text = relative.toString();
                return text.isEmpty() ? absolute.getFileName().toString() : text;
            }
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        return absolute.toString();
    }

    private String colorDiffLine(String raw) {
        if (raw.startsWith("-")) {
            return style.diffMinus(raw);
        }
        if (raw.startsWith("+")) {
            return style.diffPlus(raw);
        }
        return raw;
    }

    private static String firstLine(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int newline = text.indexOf('\n');
        return newline < 0 ? text.strip() : text.substring(0, newline).strip();
    }

    private static String afterFirstLine(String text) {
        if (text == null) {
            return "";
        }
        int newline = text.indexOf('\n');
        return newline < 0 ? "" : text.substring(newline + 1);
    }

    private static boolean looksLikeGitDiff(String text) {
        for (String line : text.split("\\R")) {
            if (line.startsWith("-   ") || line.startsWith("+   ") || line.startsWith("    ")) {
                return true;
            }
        }
        return false;
    }
}
