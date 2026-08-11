package com.ggtest.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void onlyif_trailingHashComment_parsesDbName() {
        String content = """
                onlyif sqlite # empty RHS
                """;

        List<SqlTestRecord> records = parser.parse("onlyif-hash.test", content);

        assertEquals(1, records.size());
        OnlyIfRecord only = assertInstanceOf(OnlyIfRecord.class, records.get(0));
        assertEquals("sqlite", only.dbName());
    }

    @Test
    void skipif_trailingHashComment_parsesDbName() {
        String content = """
                skipif mysql # not compatible
                """;

        List<SqlTestRecord> records = parser.parse("skipif-hash.test", content);

        assertEquals(1, records.size());
        SkipIfRecord skip = assertInstanceOf(SkipIfRecord.class, records.get(0));
        assertEquals("mysql", skip.dbName());
    }

    @Test
    void onlyifAndSkipif_withoutTrailingHash_stillParse() {
        String content = """
                skipif mysql
                onlyif sqlite
                """;

        List<SqlTestRecord> records = parser.parse("cond-plain.test", content);

        assertEquals(2, records.size());
        SkipIfRecord skip = assertInstanceOf(SkipIfRecord.class, records.get(0));
        assertEquals("mysql", skip.dbName());
        OnlyIfRecord only = assertInstanceOf(OnlyIfRecord.class, records.get(1));
        assertEquals("sqlite", only.dbName());
    }

    // --- query-head separator <delim> + exact ---- expectation header ---

    @Test
    void queryHead_separatorPipe_noLabel_bindsDelim() {
        String content = """
                query IIT nosort separator=|
                SELECT 1, 1, 'hello world'
                ----
                1 | 1 | hello world
                """;

        QueryRecord query = assertInstanceOf(QueryRecord.class, parser.parse("sep.test", content).get(0));
        assertEquals(Optional.of("|"), query.columnSeparator());
        assertEquals(Optional.empty(), query.label());
        assertEquals(List.of("1 | 1 | hello world"), query.expectedResults());
    }

    @Test
    void queryHead_multiCharDelim_allowed() {
        String content = """
                query I separator=::
                SELECT 1
                ----
                1
                """;
        QueryRecord query = assertInstanceOf(QueryRecord.class, parser.parse("multi.test", content).get(0));
        assertEquals(Optional.of("::"), query.columnSeparator());
        assertEquals(Optional.empty(), query.label());
    }

    @Test
    void queryHead_labelThenSeparator_bindsBoth() {
        String content = """
                query III nosort lbl separator=|
                SELECT 1, 2, 3
                ----
                1|2|3
                """;
        QueryRecord query = assertInstanceOf(QueryRecord.class, parser.parse("lbl-sep.test", content).get(0));
        assertEquals(Optional.of("lbl"), query.label());
        assertEquals(Optional.of("|"), query.columnSeparator());
    }

    @Test
    void p0_6_queryHead_separatorWithoutEquals_throwsHelpfulError() {
        String content = """
                query III nosort separator
                SELECT 1, 2, 3
                ----
                1
                2
                3
                """;
        ParseException ex = assertThrows(ParseException.class, () -> parser.parse("help.test", content));
        assertTrue(ex.reason().contains("key=value"), ex.reason());
    }

    @Test
    void p1_1_queryHead_separatorSpaceOldForm_throwsHelpfulError() {
        ParseException ex = assertThrows(
                ParseException.class,
                () -> parser.parse("old.test", "query III separator | extra\nSELECT 1\n"));
        assertTrue(ex.reason().contains("key=value"), ex.reason());
    }

    @Test
    void queryHead_misspelledSeperator_throwsAsLabelPlusExtraToken() {
        ParseException ex = assertThrows(
                ParseException.class,
                () -> parser.parse("typo.test", "query III nosort seperator |\nSELECT 1\n"));
        assertFalse(ex.reason().isBlank());
    }

    @Test
    void p1_4_nextQueryExactDashes_doesNotInheritSeparator() {
        String content = """
                query III nosort separator=|
                SELECT 1, 2, 3
                ----
                1|2|3

                query III
                SELECT 4, 5, 6
                ----
                4
                5
                6
                """;

        List<SqlTestRecord> records = parser.parse("scope.test", content);
        assertEquals(2, records.size());

        QueryRecord first = assertInstanceOf(QueryRecord.class, records.get(0));
        assertEquals(Optional.of("|"), first.columnSeparator());

        QueryRecord second = assertInstanceOf(QueryRecord.class, records.get(1));
        assertEquals(Optional.empty(), second.columnSeparator());
        assertEquals(List.of("4", "5", "6"), second.expectedResults());
    }

    @Test
    void p0_2_targetWriting_iitPipeBareTextQueryHead() {
        String content = """
                query IIT nosort separator=|
                SELECT 1, 1, 'hello world'
                ----
                1 | 1 | hello world
                """;

        QueryRecord query = assertInstanceOf(QueryRecord.class, parser.parse("target.test", content).get(0));
        assertEquals(Optional.of("|"), query.columnSeparator());
        assertEquals(List.of("1 | 1 | hello world"), query.expectedResults());
        assertEquals(3, query.typeSignature().size());
    }

    @Test
    void p0_7_expectationHeader_separatorRemoved_throwsReadableParseException() {
        String content = """
                query III
                SELECT 1, 2, 3
                ---- separator |
                1|2|3
                """;

        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("removed.test", content));
        assertEquals("removed.test", ex.sourceName());
        assertFalse(ex.reason().isBlank());
        String reason = ex.reason().toLowerCase();
        assertTrue(
                reason.contains("separator") && (reason.contains("removed") || reason.contains("query")),
                ex.reason());
    }

    @Test
    void expectationHeader_emptySeparatorKeyword_throwsReadableParseException() {
        String content = """
                query I
                SELECT 1
                ---- separator
                1
                """;

        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("empty-delim.test", content));
        assertFalse(ex.reason().isBlank());
        String reason = ex.reason().toLowerCase();
        assertTrue(reason.contains("separator"), ex.reason());
    }

    @Test
    void expectationHeader_trailingSpaceOnlyAfterKeyword_throws() {
        String content = "query I\nSELECT 1\n---- separator \n1\n";
        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("trail.test", content));
        assertFalse(ex.reason().isBlank());
    }

    @Test
    void expectationHeader_spaceLiteralDelim_throwsAsRemovedSyntax() {
        String content = "query I\nSELECT 1\n---- separator  \n1\n";
        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("space-delim.test", content));
        assertFalse(ex.reason().isBlank());
        assertTrue(ex.reason().toLowerCase().contains("separator"), ex.reason());
    }

    @Test
    void expectationHeader_multiCharDelim_throwsAsRemovedSyntax() {
        String content = "query I\nSELECT 1\n---- separator ::\n1\n";
        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("multi-old.test", content));
        assertFalse(ex.reason().isBlank());
        assertTrue(ex.reason().toLowerCase().contains("separator"), ex.reason());
    }

    @Test
    void topLevelSeparatorDirective_throwsReadableParseException() {
        String content = """
                ---- separator |

                query III
                SELECT 1, 2, 3
                ----
                1
                2
                3
                """;

        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("toplevel.test", content));
        assertEquals("toplevel.test", ex.sourceName());
        assertEquals(1, ex.lineNumber());
        assertFalse(ex.reason().isBlank());
        String reason = ex.reason().toLowerCase();
        assertTrue(
                reason.contains("expect") || reason.contains("query") || reason.contains("separator"),
                ex.reason());
    }

    @Test
    void illegalDashDashDashDashLine_throwsReadableParseException() {
        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("bad.test", "---- foo\n"));

        assertEquals("bad.test", ex.sourceName());
        assertEquals(1, ex.lineNumber());
        assertFalse(ex.reason().isBlank());
    }

    @Test
    void misspelledExpectationHeaderSeperator_throwsReadableParseException() {
        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("typo.test", "---- seperator |\n"));
        assertEquals(1, ex.lineNumber());
        assertFalse(ex.reason().isBlank());
    }

    @Test
    void bareSeparatorWithoutDashes_isUnknownRecordType() {
        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("bare.test", "separator |\n"));
        assertTrue(ex.reason().contains("unknown") || ex.reason().contains("separator"), ex.reason());
    }

    @Test
    void threeDashSeparator_isUnknownRecordType() {
        ParseException ex = assertThrows(
                ParseException.class, () -> parser.parse("old.test", "---separator |\n"));
        assertFalse(ex.reason().isBlank());
    }

    @Test
    void exactFourDashes_expectationHeader_valuePerLineNoSeparator() {
        String content = """
                query I
                SELECT 1
                ----
                1
                """;
        List<SqlTestRecord> records = parser.parse("boundary.test", content);
        assertEquals(1, records.size());
        QueryRecord query = assertInstanceOf(QueryRecord.class, records.get(0));
        assertTrue(query.hasExpectedResults());
        assertEquals(List.of("1"), query.expectedResults());
        assertEquals(Optional.empty(), query.columnSeparator());
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
    void pgConditionsFixture_expectedBlockEndsBeforeFollowingOnlyIf() throws Exception {
        List<SqlTestRecord> records = parser.parse(resourcePath("fixtures/pg/conditions.test"));

        QueryRecord query = records.stream()
                .filter(QueryRecord.class::isInstance)
                .map(QueryRecord.class::cast)
                .findFirst()
                .orElseThrow();
        assertEquals(List.of("7"), query.expectedResults());

        assertTrue(records.stream()
                .anyMatch(r -> r instanceof OnlyIfRecord o && o.dbName().equalsIgnoreCase("sqlite")));
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

    // --- statement error message parsing ---

    @Test
    void statementErrorWithSingleTokenMessage() {
        String content = """
                statement error no_such_table
                DROP TABLE missing
                """;

        List<SqlTestRecord> records = parser.parse("test", content);
        assertEquals(1, records.size());

        StatementRecord stmt = assertInstanceOf(StatementRecord.class, records.get(0));
        assertEquals(StatementExpectation.ERROR, stmt.expectation());
        assertEquals("no_such_table", stmt.expectedErrorMsg());
        assertEquals("DROP TABLE missing", stmt.sql());
    }

    @Test
    void statementErrorWithMultiTokenMessage() {
        String content = """
                statement error no such table: missing
                DROP TABLE missing
                """;

        List<SqlTestRecord> records = parser.parse("test", content);
        assertEquals(1, records.size());

        StatementRecord stmt = assertInstanceOf(StatementRecord.class, records.get(0));
        assertEquals(StatementExpectation.ERROR, stmt.expectation());
        assertEquals("no such table: missing", stmt.expectedErrorMsg());
    }

    @Test
    void statementErrorWithHashInMessage() {
        String content = """
                statement error table#1 not found
                DELETE FROM t1
                """;

        List<SqlTestRecord> records = parser.parse("test", content);
        assertEquals(1, records.size());

        StatementRecord stmt = assertInstanceOf(StatementRecord.class, records.get(0));
        assertEquals(StatementExpectation.ERROR, stmt.expectation());
        assertEquals("table#1 not found", stmt.expectedErrorMsg());
    }

    @Test
    void statementErrorWithoutMessageBackwardCompatible() {
        String content = """
                statement error
                DROP TABLE missing
                """;

        List<SqlTestRecord> records = parser.parse("test", content);
        assertEquals(1, records.size());

        StatementRecord stmt = assertInstanceOf(StatementRecord.class, records.get(0));
        assertEquals(StatementExpectation.ERROR, stmt.expectation());
        assertNull(stmt.expectedErrorMsg());
    }

    @Test
    void statementOkRejectsExtraTokens() {
        String content = """
                statement ok extra
                SELECT 1
                """;

        ParseException ex = assertThrows(ParseException.class, () -> parser.parse("test", content));
        assertTrue(ex.getMessage().contains("statement ok does not take additional operands"));
    }

    // --- expected interval fields (override D1) ---

    @Test
    void queryExpectedInterval_recordsHeaderAndBodyEndLines() {
        String content = """
                statement ok
                CREATE TABLE t(a INT)

                query I nosort
                SELECT a FROM t
                ----
                1
                2

                query T nosort
                SELECT 'x'
                ----
                x
                """;

        List<SqlTestRecord> records = parser.parse("iv.test", content);

        QueryRecord first = assertInstanceOf(QueryRecord.class, records.get(1));
        assertTrue(first.hasExpectedResults());
        assertEquals(6, first.expectedHeaderLine(), "---- is on line 6");
        assertEquals(8, first.expectedBodyEndLine(), "last body line (2) is on line 8");

        QueryRecord second = assertInstanceOf(QueryRecord.class, records.get(2));
        assertTrue(second.hasExpectedResults());
        assertEquals(12, second.expectedHeaderLine());
        assertEquals(13, second.expectedBodyEndLine());
    }

    @Test
    void executeOnlyQueryHasZeroInterval() {
        String content = """
                query I nosort
                SELECT 1
                """;

        QueryRecord query = assertInstanceOf(QueryRecord.class, parser.parse("eo.test", content).get(0));

        assertFalse(query.hasExpectedResults());
        assertEquals(0, query.expectedHeaderLine());
        assertEquals(0, query.expectedBodyEndLine());
    }

    @Test
    void emptyExpectedBlockHasBodyStartExceedingEnd() {
        String content = "query I nosort\nSELECT 1\n----\n\n";

        QueryRecord query = assertInstanceOf(QueryRecord.class, parser.parse("empty.test", content).get(0));

        assertTrue(query.hasExpectedResults());
        assertTrue(query.expectedResults().isEmpty());
        assertEquals(3, query.expectedHeaderLine());
        assertEquals(3, query.expectedBodyEndLine(), "empty block: end == header line");
        assertTrue(query.expectedHeaderLine() + 1 > query.expectedBodyEndLine());
    }

    @Test
    void statementErrorMsgStartColumn_singleMessage() {
        String content = """
                statement error no such table
                SELECT * FROM missing
                """;

        StatementRecord stmt = assertInstanceOf(
                StatementRecord.class, parser.parse("msg.test", content).get(0));

        assertEquals("no such table", stmt.expectedErrorMsg());
        assertEquals(16, stmt.errorMsgStartColumn());
    }

    @Test
    void statementErrorMsgStartColumn_leadingWhitespace() {
        String content = """
                  statement error   no such table
                SELECT * FROM missing
                """;

        StatementRecord stmt = assertInstanceOf(
                StatementRecord.class, parser.parse("ws.test", content).get(0));

        assertEquals("no such table", stmt.expectedErrorMsg());
        assertEquals("  statement error   ".length(), stmt.errorMsgStartColumn());
    }

    @Test
    void statementErrorWithoutMessageHasMinusOneColumn() {
        String content = """
                statement error
                SELECT * FROM missing
                """;

        StatementRecord stmt = assertInstanceOf(
                StatementRecord.class, parser.parse("nomsg.test", content).get(0));

        assertNull(stmt.expectedErrorMsg());
        assertEquals(-1, stmt.errorMsgStartColumn());
    }

    @Test
    void statementOkHasMinusOneColumn() {
        String content = """
                statement ok
                SELECT 1
                """;

        StatementRecord stmt = assertInstanceOf(
                StatementRecord.class, parser.parse("ok.test", content).get(0));

        assertEquals(-1, stmt.errorMsgStartColumn());
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
                    + q.expectedResults()
                    + "|"
                    + q.columnSeparator();
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
