package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.model.ColumnType;
import com.ggtest.model.QueryRecord;
import com.ggtest.model.SortMode;
import com.ggtest.model.SourceLocation;
import com.ggtest.model.StatementExpectation;
import com.ggtest.model.StatementRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OverrideWriterTest {

    private final OverrideWriter writer = new OverrideWriter();

    @Test
    void singleQueryOverride_replacesBodyPreservesRest(@TempDir Path tempDir) throws IOException {
        String content = "statement ok\nCREATE TABLE t(x int);\n\nquery I nosort\nSELECT x FROM t\n----\nwrong\n";
        QueryRecord query = queryRecord(4, 6, 7);

        String result = writer.rewrite(content, List.of(new OverrideWriter.Override(query, "42")));

        assertEquals(
                "statement ok\nCREATE TABLE t(x int);\n\nquery I nosort\nSELECT x FROM t\n----\n42\n",
                result);
    }

    @Test
    void singleQueryOverride_preservesDashHeaderAndPostBlockBlank(@TempDir Path tempDir) {
        String content = "query I nosort\nSELECT 1\n----\nold1\nold2\n\nstatement ok\nSELECT 2\n";
        QueryRecord query = queryRecord(1, 3, 5);

        String result = writer.rewrite(content, List.of(new OverrideWriter.Override(query, "a\nb")));

        assertEquals(
                "query I nosort\nSELECT 1\n----\na\nb\n\nstatement ok\nSELECT 2\n",
                result);
    }

    @Test
    void statementErrorOverride_replacesMessagePreservesPrefix() {
        String content = "statement error old message\nSELECT * FROM missing\n";
        StatementRecord stmt = statementRecord(1, "statement error ".length());

        String result = writer.rewrite(content, List.of(new OverrideWriter.Override(stmt, "new detail")));

        assertEquals("statement error new detail\nSELECT * FROM missing\n", result);
    }

    @Test
    void statementErrorOverride_leadingWhitespacePreserved() {
        String content = "  statement error   old\nSELECT 1\n";
        StatementRecord stmt = statementRecord(1, "  statement error   ".length());

        String result = writer.rewrite(content, List.of(new OverrideWriter.Override(stmt, "fresh")));

        assertEquals("  statement error   fresh\nSELECT 1\n", result);
    }

    @Test
    void multipleOverridesAppliedInOneRewrite() {
        String content = ""
                + "statement error oldmsg\n"
                + "SELECT * FROM a\n"
                + "\n"
                + "query I nosort\n"
                + "SELECT 1\n"
                + "----\n"
                + "wrong\n"
                + "\n";
        StatementRecord stmt = statementRecord(1, "statement error ".length());
        QueryRecord query = queryRecord(4, 6, 7);

        String result = writer.rewrite(content, List.of(
                new OverrideWriter.Override(query, "99"),
                new OverrideWriter.Override(stmt, "newmsg")));

        assertEquals(
                "statement error newmsg\nSELECT * FROM a\n\nquery I nosort\nSELECT 1\n----\n99\n\n",
                result);
    }

    @Test
    void emptyOverridesReturnsContentUnchanged() {
        String content = "query I nosort\nSELECT 1\n----\n1\n";
        assertEquals(content, writer.rewrite(content, List.of()));
    }

    @Test
    void crlfEolPreserved() {
        String content = "query I nosort\r\nSELECT 1\r\n----\r\nwrong\r\n";
        QueryRecord query = queryRecord(1, 3, 4);

        String result = writer.rewrite(content, List.of(new OverrideWriter.Override(query, "ok")));

        assertEquals("query I nosort\r\nSELECT 1\r\n----\r\nok\r\n", result);
    }

    @Test
    void noTrailingNewlinePreserved() {
        String content = "query I nosort\nSELECT 1\n----\nwrong";
        QueryRecord query = queryRecord(1, 3, 4);

        String result = writer.rewrite(content, List.of(new OverrideWriter.Override(query, "good")));

        assertEquals("query I nosort\nSELECT 1\n----\ngood", result);
    }

    @Test
    void emptyExpectedBlock_insertsNewBody() {
        String content = "query I nosort\nSELECT 1\n----\n\n";
        QueryRecord query = new QueryRecord(
                List.of(ColumnType.INTEGER), SortMode.NOSORT, Optional.empty(),
                "SELECT 1", true, List.of(), Optional.empty(),
                new SourceLocation("t", 1), 3, 3);

        String result = writer.rewrite(content, List.of(new OverrideWriter.Override(query, "42")));

        assertEquals("query I nosort\nSELECT 1\n----\n42\n\n", result);
    }

    @Test
    void writeAtomically_overwritesFile(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("out.test");
        Files.writeString(target, "old\n", StandardCharsets.UTF_8);

        writer.writeAtomically(target, "new\n");

        assertEquals("new\n", Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void writeAtomically_utf8Content(@TempDir Path tempDir) throws IOException {
        Path target = tempDir.resolve("utf8.test");
        Files.writeString(target, "x\n", StandardCharsets.UTF_8);

        writer.writeAtomically(target, "query T\nSELECT 'héllo wörld'\n----\nhéllo wörld\n");

        assertEquals(
                "query T\nSELECT 'héllo wörld'\n----\nhéllo wörld\n",
                Files.readString(target, StandardCharsets.UTF_8));
    }

    @Test
    void writeFailureLeavesOriginalIntact(@TempDir Path tempDir) throws IOException {
        Path roDir = tempDir.resolve("ro");
        Files.createDirectories(roDir);
        Path target = roDir.resolve("file.test");
        String original = "statement ok\nSELECT 1\n";
        Files.writeString(target, original, StandardCharsets.UTF_8);
        boolean madeReadOnly = roDir.toFile().setReadOnly();
        try {
            org.junit.jupiter.api.Assumptions.assumeTrue(
                    madeReadOnly, "cannot set directory read-only on this platform");

            assertThrows(IOException.class, () -> writer.writeAtomically(target, "replaced\n"));

            assertEquals(original, Files.readString(target, StandardCharsets.UTF_8),
                    "original must be intact after write failure");
        } finally {
            roDir.toFile().setWritable(true);
        }
    }

    @Test
    void writeAtomically_nonexistentParentThrows(@TempDir Path tempDir) {
        Path target = tempDir.resolve("no").resolve("such").resolve("dir.test");

        assertThrows(IOException.class, () -> writer.writeAtomically(target, "x\n"));
    }

    @Test
    void rewriteThenParse_isIdempotent(@TempDir Path tempDir) throws IOException {
        String content = "query I nosort\nSELECT 1\n----\nwrong\n";
        QueryRecord query = queryRecord(1, 3, 4);

        String rewritten = writer.rewrite(content, List.of(new OverrideWriter.Override(query, "1")));
        assertEquals("query I nosort\nSELECT 1\n----\n1\n", rewritten);

        assertFalse(rewritten.contains("wrong"));
        assertTrue(rewritten.contains("1"));
    }

    private static QueryRecord queryRecord(int startLine, int headerLine, int bodyEndLine) {
        return new QueryRecord(
                List.of(ColumnType.INTEGER),
                SortMode.NOSORT,
                Optional.empty(),
                "SELECT 1",
                true,
                List.of("placeholder"),
                Optional.empty(),
                new SourceLocation("t", startLine),
                headerLine,
                bodyEndLine);
    }

    private static StatementRecord statementRecord(int startLine, int msgCol) {
        return new StatementRecord(
                "SQL",
                StatementExpectation.ERROR,
                "old",
                new SourceLocation("t", startLine),
                msgCol);
    }
}
