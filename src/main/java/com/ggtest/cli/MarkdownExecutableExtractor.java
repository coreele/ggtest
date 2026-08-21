package com.ggtest.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Converts executable Markdown fences into a line-preserving sqllogictest view. */
final class MarkdownExecutableExtractor {

    private static final FenceBlockLanguageRegistry LANGUAGES =
            new FenceBlockLanguageRegistry(Set.of("sql", "slt", "sqllogictest"));

    private MarkdownExecutableExtractor() {}

    static String toExecutableSlt(String markdown) {
        Objects.requireNonNull(markdown, "markdown");
        return toExecutableSlt(markdown, LANGUAGES);
    }

    static String toExecutableSlt(String markdown, FenceBlockLanguageRegistry languages) {
        Objects.requireNonNull(markdown, "markdown");
        Objects.requireNonNull(languages, "languages");
        List<String> sourceLines = splitPreserveParserLines(markdown);
        if (sourceLines.isEmpty()) {
            return "";
        }

        List<String> executableLines = new ArrayList<>(sourceLines.size());
        boolean inFence = false;
        boolean executableFence = false;

        for (String line : sourceLines) {
            FenceLine fence = FenceLine.parse(line);
            if (fence != null) {
                if (inFence) {
                    inFence = false;
                    executableFence = false;
                } else {
                    inFence = true;
                    executableFence = languages.mapsToSqllogictest(fence.language());
                }
                executableLines.add("");
            } else if (inFence && executableFence) {
                executableLines.add(line);
            } else {
                executableLines.add("");
            }
        }

        StringBuilder out = new StringBuilder();
        for (String executableLine : executableLines) {
            out.append(executableLine).append('\n');
        }
        return out.toString();
    }

    private static List<String> splitPreserveParserLines(String content) {
        List<String> result = new ArrayList<>();
        if (content.isEmpty()) {
            return result;
        }
        int begin = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                result.add(stripTrailingCr(content.substring(begin, i)));
                begin = i + 1;
            }
        }
        if (begin < content.length()) {
            result.add(stripTrailingCr(content.substring(begin)));
        }
        return result;
    }

    private static String stripTrailingCr(String line) {
        if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') {
            return line.substring(0, line.length() - 1);
        }
        return line;
    }

    record FenceLine(String language) {

        private static FenceLine parse(String line) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("```")) {
                return null;
            }
            int ticks = 0;
            while (ticks < trimmed.length() && trimmed.charAt(ticks) == '`') {
                ticks++;
            }
            if (ticks < 3) {
                return null;
            }
            String info = trimmed.substring(ticks).strip();
            return new FenceLine(firstToken(info));
        }

        private static String firstToken(String info) {
            if (info.isEmpty()) {
                return "";
            }
            int end = 0;
            while (end < info.length() && !Character.isWhitespace(info.charAt(end))) {
                end++;
            }
            return info.substring(0, end);
        }
    }
}

record FenceBlockLanguageRegistry(Set<String> sqllogictestLanguages) {

    FenceBlockLanguageRegistry {
        Objects.requireNonNull(sqllogictestLanguages, "sqllogictestLanguages");
        sqllogictestLanguages = Set.copyOf(sqllogictestLanguages);
    }

    boolean mapsToSqllogictest(String language) {
        return sqllogictestLanguages.contains(language.toLowerCase(Locale.ROOT));
    }
}
