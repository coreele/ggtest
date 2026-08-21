package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkdownExecutableExtractorTest {

    @Test
    void emptyMarkdownProducesEmptyExecutableView() {
        assertEquals("", MarkdownExecutableExtractor.toExecutableSlt(""));
    }

    @Test
    void supportedFenceContentsAreKeptOnOriginalLines() {
        String markdown = ""
                + "# Title\n"
                + "\n"
                + "```sql\n"
                + "statement ok\n"
                + "SELECT 1\n"
                + "```\n"
                + "after\n";

        String executable = MarkdownExecutableExtractor.toExecutableSlt(markdown);

        assertEquals(
                List.of("", "", "", "statement ok", "SELECT 1", "", ""),
                parserLines(executable));
    }

    @Test
    void supportedLanguagesUseFirstInfoTokenCaseInsensitively() {
        String markdown = ""
                + "```SQL title\n"
                + "statement ok\n"
                + "SELECT 1\n"
                + "```\n"
                + "```slt\n"
                + "query I nosort\n"
                + "SELECT 2\n"
                + "----\n"
                + "2\n"
                + "```\n"
                + "```sqllogictest\n"
                + "statement ok\n"
                + "SELECT 3\n"
                + "```\n";

        String executable = MarkdownExecutableExtractor.toExecutableSlt(markdown);

        assertEquals(
                List.of(
                        "",
                        "statement ok",
                        "SELECT 1",
                        "",
                        "",
                        "query I nosort",
                        "SELECT 2",
                        "----",
                        "2",
                        "",
                        "",
                        "statement ok",
                        "SELECT 3",
                        ""),
                parserLines(executable));
    }

    @Test
    void unsupportedFenceContentsAreMasked() {
        String markdown = ""
                + "before\n"
                + "```python\n"
                + "statement ok\n"
                + "SELECT 1\n"
                + "```\n"
                + "```sqlx\n"
                + "query I nosort\n"
                + "SELECT 2\n"
                + "```\n";

        String executable = MarkdownExecutableExtractor.toExecutableSlt(markdown);

        assertFalse(executable.contains("statement ok"));
        assertFalse(executable.contains("query I"));
        assertEquals(List.of("", "", "", "", "", "", "", "", ""), parserLines(executable));
    }

    @Test
    void unclosedSupportedFenceRunsToEof() {
        String markdown = ""
                + "intro\n"
                + "```slt\n"
                + "statement ok\n"
                + "SELECT 1\n";

        String executable = MarkdownExecutableExtractor.toExecutableSlt(markdown);

        assertEquals(List.of("", "", "statement ok", "SELECT 1"), parserLines(executable));
    }

    private static List<String> parserLines(String content) {
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
}
