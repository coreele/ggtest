package com.ggtest.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.model.ColumnType;
import com.ggtest.model.HaltRecord;
import com.ggtest.model.HashThresholdRecord;
import com.ggtest.model.OnlyIfRecord;
import com.ggtest.model.QueryRecord;
import com.ggtest.model.SkipIfRecord;
import com.ggtest.model.SortMode;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.model.StatementExpectation;
import com.ggtest.model.StatementRecord;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Acceptance coverage for parser slice: P0-7, P1-a, P1-b, P1-c.
 */
class SqlLogicTestParserTest {

    private SqlLogicTestParser parser;

    @BeforeEach
    void setUp() {
        parser = new SqlLogicTestParser();
    }

    // --- P0-7: parse error location ---

    @Test
    void p0_7_unknownRecordType_messageContainsSourceNameAndLineNumber() {
        String content = """
                # leading comment

                statement ok
                CREATE TABLE t(a INT)

                unknown-type xyz
                """;

        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("bad.test", content));

        assertEquals("bad.test", ex.sourceName());
        assertEquals(6, ex.lineNumber());
        assertEquals("bad.test:6: " + ex.reason(), ex.getMessage());
        assertTrue(ex.getMessage().contains("bad.test"));
        assertTrue(ex.getMessage().contains("6"));
        assertFalse(ex.reason().isBlank());
    }

    @Test
    void p0_7_unknownRecordType_fromFixtureFile() throws Exception {
        Path fixture = resourcePath("fixtures/unknown-record.test");
        ParseException ex = assertThrows(ParseException.class, () -> parser.parse(fixture));

        String sourceName = fixture.toString();
        assertEquals(sourceName, ex.sourceName());
        assertEquals(3, ex.lineNumber());
        assertEquals(sourceName + ":3: " + ex.reason(), ex.getMessage());
    }

    @Test
    void illegalTypeSignature_throwsParseExceptionWithLine() {
        String content = """
                query X
                SELECT 1
                """;

        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("sig.test", content));

        assertEquals("sig.test", ex.sourceName());
        assertEquals(1, ex.lineNumber());
        assertTrue(ex.getMessage().startsWith("sig.test:1:"));
    }

    @Test
    void truncatedStatement_throwsParseException() {
        String content = "statement ok\n";

        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("trunc.test", content));

        assertEquals("trunc.test", ex.sourceName());
        assertEquals(1, ex.lineNumber());
        assertTrue(ex.getMessage().startsWith("trunc.test:1:"));
    }

    // --- P1-a: record types and comments ---

    @Test
    void p1_a_allRecordTypes_commentsAndBlankLinesIgnored() {
        String content = """
                # file comment

                # before statement
                statement ok
                CREATE TABLE t(a INT)

                statement error
                DROP TABLE missing

                query II rowsort label1
                SELECT a, b FROM t
                ----
                1 2
                3 4

                skipif mysql
                onlyif sqlite
                hash-threshold 8
                halt
                """;

        List<SqlTestRecord> records = parser.parse("all.test", content);

        assertEquals(7, records.size());

        StatementRecord ok = assertInstanceOf(StatementRecord.class, records.get(0));
        assertEquals(StatementExpectation.OK, ok.expectation());
        assertEquals("CREATE TABLE t(a INT)", ok.sql());
        assertEquals("all.test", ok.location().sourceName());
        assertEquals(4, ok.location().startLine());

        StatementRecord err = assertInstanceOf(StatementRecord.class, records.get(1));
        assertEquals(StatementExpectation.ERROR, err.expectation());
        assertEquals("DROP TABLE missing", err.sql());
        assertEquals(7, err.location().startLine());

        QueryRecord query = assertInstanceOf(QueryRecord.class, records.get(2));
        assertEquals(List.of(ColumnType.INTEGER, ColumnType.INTEGER), query.typeSignature());
        assertEquals(SortMode.ROWSORT, query.sortMode());
        assertEquals(Optional.of("label1"), query.label());
        assertEquals("SELECT a, b FROM t", query.sql());
        assertTrue(query.hasExpectedResults());
        assertEquals(List.of("1 2", "3 4"), query.expectedResults());
        assertEquals(10, query.location().startLine());

        SkipIfRecord skip = assertInstanceOf(SkipIfRecord.class, records.get(3));
        assertEquals("mysql", skip.dbName());
        assertEquals(16, skip.location().startLine());

        OnlyIfRecord only = assertInstanceOf(OnlyIfRecord.class, records.get(4));
        assertEquals("sqlite", only.dbName());
        assertEquals(17, only.location().startLine());

        HashThresholdRecord hash = assertInstanceOf(HashThresholdRecord.class, records.get(5));
        assertEquals(8, hash.threshold());
        assertEquals(18, hash.location().startLine());

        HaltRecord halt = assertInstanceOf(HaltRecord.class, records.get(6));
        assertEquals(19, halt.location().startLine());
    }

    @Test
    void p1_a_fromFixtureFile() throws Exception {
        Path fixture = resourcePath("fixtures/all-records.test");
        List<SqlTestRecord> records = parser.parse(fixture);
        assertEquals(7, records.size());
        assertInstanceOf(StatementRecord.class, records.get(0));
        assertInstanceOf(StatementRecord.class, records.get(1));
        assertInstanceOf(QueryRecord.class, records.get(2));
        assertInstanceOf(SkipIfRecord.class, records.get(3));
        assertInstanceOf(OnlyIfRecord.class, records.get(4));
        assertInstanceOf(HashThresholdRecord.class, records.get(5));
        assertInstanceOf(HaltRecord.class, records.get(6));
    }

    // --- P1-b: query without ---- ---

    @Test
    void p1_b_queryWithoutSeparator_executeOnly() {
        String content = """
                query I
                SELECT 1
                """;

        List<SqlTestRecord> records = parser.parse("exec-only.test", content);

        assertEquals(1, records.size());
        QueryRecord query = assertInstanceOf(QueryRecord.class, records.get(0));
        assertEquals(List.of(ColumnType.INTEGER), query.typeSignature());
        assertEquals(SortMode.NOSORT, query.sortMode());
        assertEquals(Optional.empty(), query.label());
        assertEquals("SELECT 1", query.sql());
        assertFalse(query.hasExpectedResults());
        assertTrue(query.expectedResults().isEmpty());
    }

    // --- P1-c: extension-independent ---

    @Test
    void p1_c_sameContent_testSltAndNoSuffix_equivalent(@TempDir Path tempDir)
            throws IOException, URISyntaxException {
        String content = readResource("fixtures/equivalent-content.test");

        Path testFile = tempDir.resolve("sample.test");
        Path sltFile = tempDir.resolve("sample.slt");
        Path noSuffix = tempDir.resolve("sample");
        Files.writeString(testFile, content, StandardCharsets.UTF_8);
        Files.writeString(sltFile, content, StandardCharsets.UTF_8);
        Files.writeString(noSuffix, content, StandardCharsets.UTF_8);

        List<SqlTestRecord> fromTest = parser.parse(testFile);
        List<SqlTestRecord> fromSlt = parser.parse(sltFile);
        List<SqlTestRecord> fromNoSuffix = parser.parse(noSuffix);

        assertRecordsSemanticallyEqual(fromTest, fromSlt);
        assertRecordsSemanticallyEqual(fromTest, fromNoSuffix);
    }

    @Test
    void p1_c_resourceFixtures_testSltNoSuffix_equivalent() throws Exception {
        List<SqlTestRecord> fromTest = parser.parse(resourcePath("fixtures/equivalent-content.test"));
        List<SqlTestRecord> fromSlt = parser.parse(resourcePath("fixtures/equivalent-content.slt"));
        List<SqlTestRecord> fromNoSuffix = parser.parse(resourcePath("fixtures/equivalent-content"));

        assertRecordsSemanticallyEqual(fromTest, fromSlt);
        assertRecordsSemanticallyEqual(fromTest, fromNoSuffix);
    }

    @Test
    void parse_stringSource_preservesOrderAndMultilineSql() {
        String content = """
                statement ok
                CREATE TABLE t(a INT);
                INSERT INTO t VALUES (1)

                query I valuesort
                SELECT a
                FROM t
                ----
                1
                """;

        List<SqlTestRecord> records = parser.parse("multi.test", content);
        assertEquals(2, records.size());

        StatementRecord stmt = assertInstanceOf(StatementRecord.class, records.get(0));
        assertEquals("CREATE TABLE t(a INT);\nINSERT INTO t VALUES (1)", stmt.sql());

        QueryRecord query = assertInstanceOf(QueryRecord.class, records.get(1));
        assertEquals(SortMode.VALUESORT, query.sortMode());
        assertEquals("SELECT a\nFROM t", query.sql());
        assertTrue(query.hasExpectedResults());
        assertEquals(List.of("1"), query.expectedResults());
    }

    private static void assertRecordsSemanticallyEqual(
            List<SqlTestRecord> left, List<SqlTestRecord> right) {
        assertEquals(left.size(), right.size());
        for (int i = 0; i < left.size(); i++) {
            assertEquals(semanticKey(left.get(i)), semanticKey(right.get(i)), "record index " + i);
        }
    }

    private static String semanticKey(SqlTestRecord record) {
        if (record instanceof StatementRecord s) {
            return "statement|" + s.expectation() + "|" + s.sql();
        }
        if (record instanceof QueryRecord q) {
            return "query|"
                    + q.typeSignature()
                    + "|"
                    + q.sortMode()
                    + "|"
                    + q.label()
                    + "|"
                    + q.sql()
                    + "|"
                    + q.hasExpectedResults()
                    + "|"
                    + q.expectedResults();
        }
        if (record instanceof SkipIfRecord s) {
            return "skipif|" + s.dbName();
        }
        if (record instanceof OnlyIfRecord o) {
            return "onlyif|" + o.dbName();
        }
        if (record instanceof HashThresholdRecord h) {
            return "hash-threshold|" + h.threshold();
        }
        if (record instanceof HaltRecord) {
            return "halt";
        }
        throw new IllegalStateException("unexpected record: " + record.getClass());
    }

    private static Path resourcePath(String resource) throws URISyntaxException {
        var url = SqlLogicTestParserTest.class.getClassLoader().getResource(resource);
        if (url == null) {
            throw new IllegalStateException("missing resource: " + resource);
        }
        return Path.of(url.toURI());
    }

    private static String readResource(String resource) throws IOException, URISyntaxException {
        return Files.readString(resourcePath(resource), StandardCharsets.UTF_8);
    }
}
