package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.model.ColumnType;
import com.ggtest.model.QueryRecord;
import com.ggtest.model.SortMode;
import com.ggtest.model.SourceLocation;
import com.ggtest.model.StatementExpectation;
import com.ggtest.model.StatementRecord;
import com.ggtest.runner.RecordOutcome;
import com.ggtest.runner.RecordResult;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ReportWriter} formatting (aligned with CliReportAcceptanceTest P0-2). */
class ReportWriterTest {

    private static final Pattern ANSI = Pattern.compile("\\u001B\\[[0-9;]*m");

    private ReportWriter writer;
    private ByteArrayOutputStream buffer;

    @BeforeEach
    void setUp() {
        buffer = new ByteArrayOutputStream();
        writer = new ReportWriter(new PrintStream(buffer), new ReportStyle(false));
    }

    @Test
    void resultMismatchFormatsAtAndDiff() {
        String diffBody = "-   apple\n+   bananad\n    cherry";
        RecordResult recordResult = new RecordResult(
                query("SELECT name\nFROM items", 7),
                RecordOutcome.FAILED,
                "result mismatch:\n" + diffBody);

        List<String> lines = writer.formatFailureDetailLines("fail.test", recordResult);
        String joined = stripAnsi(String.join("\n", lines));

        assertTrue(joined.contains("    at fail.test:7 : query result mismatch"), "first line: " + joined);
        assertTrue(joined.contains("(-expected|+actual)"));
        assertFalse(joined.contains("[WHY]"));
        assertFalse(joined.contains("[SQL]"));
        assertFalse(joined.contains("[Diff]"));
        assertTrue(joined.contains("-   apple"));
        assertTrue(joined.contains("+   bananad"));
        assertBodyIndent(lines);
    }

    @Test
    void atLineHasFourSpaceIndent() {
        List<String> withLine = writer.detailLines("why", null, "f.test", 9);
        List<String> withoutLine = writer.detailLines("why", null, "f.test", null);

        assertEquals("    at f.test:9 : why", stripAnsi(withLine.get(0)));
        assertEquals("    at f.test : why", stripAnsi(withoutLine.get(0)));
    }

    @Test
    void gitDiffReasonUsesAtWithDiff() {
        String reason = "statement failed\n-   expected\n+   actual";
        RecordResult recordResult = new RecordResult(
                statement("INSERT INTO t VALUES (1)", 3),
                RecordOutcome.FAILED,
                reason);

        List<String> lines = writer.formatFailureDetailLines("stmt.test", recordResult);
        String joined = stripAnsi(String.join("\n", lines));

        assertTrue(joined.contains("    at stmt.test:3 : statement failed"), "first line: " + joined);
        assertTrue(joined.contains("(-expected|+actual)"));
        assertFalse(joined.contains("[WHY]"));
        assertFalse(joined.contains("[SQL]"));
        assertFalse(joined.contains("[Diff]"));
    }

    @Test
    void hardErrorDetailOmitDiff() {
        List<String> lines = writer.detailLines("parse error: bad token", null, "bad.test", 1);
        String joined = stripAnsi(String.join("\n", lines));

        assertTrue(joined.contains("    at bad.test:1 : parse error: bad token"), "first line: " + joined);
        assertFalse(joined.contains("[WHY]"));
        assertFalse(joined.contains("[SQL]"));
        assertFalse(joined.contains("[Diff]"));
        assertFalse(joined.contains("(-expected|+actual)"));
        assertEquals(1, lines.size(), "no diff so only at line");
    }

    @Test
    void skipTimingStatusLineOmitsElapsed() {
        writer.printStatusLine("skip.test", 60, "[SKIPPED]", 42L, false);
        String line = stripAnsi(buffer.toString());

        assertTrue(line.contains("skip.test"));
        assertTrue(line.contains("[SKIPPED]"));
        assertFalse(line.contains(" in "));
        assertFalse(line.contains("ms"));
    }

    @Test
    void coloredDiffUsesAnsiWhenEnabled() {
        ReportWriter colored = new ReportWriter(new PrintStream(buffer), new ReportStyle(true));
        RecordResult recordResult = new RecordResult(
                query("SELECT 1", 1),
                RecordOutcome.FAILED,
                "result mismatch:\n-   a\n+   b");

        List<String> lines = colored.formatFailureDetailLines("x.test", recordResult);
        String joined = String.join("\n", lines);

        assertTrue(ANSI.matcher(joined).find(), "diff lines should include ANSI when enabled");
    }

    private static QueryRecord query(String sql, int line) {
        return new QueryRecord(
                List.of(ColumnType.TEXT),
                SortMode.NOSORT,
                Optional.empty(),
                sql,
                true,
                List.of("a"),
                Optional.empty(),
                new SourceLocation("fail.test", line));
    }

    private static StatementRecord statement(String sql, int line) {
        return new StatementRecord(sql, StatementExpectation.OK, new SourceLocation("stmt.test", line));
    }

    private static void assertBodyIndent(List<String> lines) {
        assertFalse(lines.isEmpty());
        String first = stripAnsi(lines.get(0));
        assertTrue(first.startsWith("    at "), "first line must be indented at: " + first);
        for (int i = 1; i < lines.size(); i++) {
            String line = stripAnsi(lines.get(i));
            if (line.isEmpty()) {
                continue;
            }
            assertTrue(line.startsWith("        "), "diff lines must have eight-space indent: [" + line + "]");
        }
    }

    private static String stripAnsi(String text) {
        return ANSI.matcher(text).replaceAll("");
    }
}
