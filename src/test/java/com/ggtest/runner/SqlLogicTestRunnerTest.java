package com.ggtest.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.model.ColumnType;
import com.ggtest.model.HaltRecord;
import com.ggtest.model.HashThresholdRecord;
import com.ggtest.model.OnlyIfRecord;
import com.ggtest.model.QueryRecord;
import com.ggtest.model.SkipIfRecord;
import com.ggtest.model.SortMode;
import com.ggtest.model.SourceLocation;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.model.StatementExpectation;
import com.ggtest.model.StatementRecord;
import com.ggtest.normalize.ResultHasher;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SqlLogicTestRunnerTest {

    private static final String SOURCE = "orchestration.test";

    private int nextLine = 1;

    @Test
    void statementOkPassesWhenExecutionSucceeds() {
        FileRunResult result = run(new FakeDatabaseExecutor(), statementOk("CREATE TABLE t1(a INTEGER)"));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(result));
        assertEquals(1, result.passedCount());
    }

    @Test
    void statementOkFailsWithErrorMaterialWhenExecutionFails() {
        FakeDatabaseExecutor executor =
                new FakeDatabaseExecutor().statementFails("INSERT INTO missing VALUES(1)", "no such table: missing");

        FileRunResult result = run(executor, statementOk("INSERT INTO missing VALUES(1)"));

        assertEquals(List.of(RecordOutcome.FAILED), outcomes(result));
        assertTrue(result.recordResults().get(0).failureReason().contains("no such table: missing"));
    }

    @Test
    void statementErrorPassesWhenExecutionFails() {
        FakeDatabaseExecutor executor =
                new FakeDatabaseExecutor().statementFails("INSERT INTO missing VALUES(1)", "no such table: missing");

        FileRunResult result = run(executor, statementError("INSERT INTO missing VALUES(1)"));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(result));
    }

    @Test
    void statementErrorFailsWhenExecutionSucceeds() {
        FileRunResult result = run(new FakeDatabaseExecutor(), statementError("SELECT 1"));

        assertEquals(List.of(RecordOutcome.FAILED), outcomes(result));
        assertFalse(result.recordResults().get(0).failureReason().isEmpty());
    }

    @Test
    void failingRecordDoesNotStopRemainingRecords() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor().statementFails("BOOM", "syntax error");

        FileRunResult result = run(executor, statementOk("BOOM"), statementOk("SELECT 1"));

        assertEquals(List.of(RecordOutcome.FAILED, RecordOutcome.PASSED), outcomes(result));
        assertEquals(List.of("BOOM", "SELECT 1"), executor.executedSql());
        assertFalse(result.aborted());
    }

    @Test
    void conditionsSkipOrRunNextRecordPerEngine() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor();

        FileRunResult result = run(
                executor,
                skipIf("sqlite"),
                statementOk("SELECT 'skipped by skipif'"),
                onlyIf("sqlite"),
                statementOk("SELECT 'runs on sqlite'"),
                onlyIf("postgresql"),
                statementOk("SELECT 'skipped by onlyif'"));

        assertEquals(
                List.of(RecordOutcome.SKIPPED, RecordOutcome.PASSED, RecordOutcome.SKIPPED),
                outcomes(result));
        assertEquals(List.of("SELECT 'runs on sqlite'"), executor.executedSql());
        assertEquals(2, result.skippedCount());
    }

    @Test
    void engineMatchingIgnoresCase() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor();

        FileRunResult result = run(executor, skipIf("SQLite"), statementOk("SELECT 1"));

        assertEquals(List.of(RecordOutcome.SKIPPED), outcomes(result));
        assertEquals(List.of(), executor.executedSql());
    }

    @Test
    void stackedConditionsApplyToNextRecordOnlyThenClear() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor();

        FileRunResult result = run(
                executor,
                skipIf("postgresql"),
                onlyIf("sqlite"),
                skipIf("sqlite"),
                statementOk("SELECT 'guarded'"),
                statementOk("SELECT 'unguarded'"));

        assertEquals(List.of(RecordOutcome.SKIPPED, RecordOutcome.PASSED), outcomes(result));
        assertEquals(List.of("SELECT 'unguarded'"), executor.executedSql());
    }

    @Test
    void danglingConditionAtEndOfFileIsHarmless() {
        FileRunResult result = run(new FakeDatabaseExecutor(), statementOk("SELECT 1"), skipIf("sqlite"));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(result));
    }

    @Test
    void haltSkipsRemainingRecords() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor();

        FileRunResult result = run(
                executor,
                statementOk("SELECT 'before halt'"),
                halt(),
                statementOk("SELECT 'after halt'"),
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, null, "SELECT 2", List.of("2")));

        assertEquals(
                List.of(RecordOutcome.PASSED, RecordOutcome.SKIPPED, RecordOutcome.SKIPPED),
                outcomes(result));
        assertTrue(result.halted());
        assertFalse(result.aborted());
        assertEquals(List.of("SELECT 'before halt'"), executor.executedSql());
        assertEquals(2, result.skippedCount());
    }

    @Test
    void skippedHaltDoesNotStopFile() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor();

        FileRunResult result = run(
                executor, onlyIf("postgresql"), halt(), statementOk("SELECT 'still runs'"));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(result));
        assertFalse(result.halted());
        assertEquals(List.of("SELECT 'still runs'"), executor.executedSql());
    }

    @Test
    void hashThresholdRecordSwitchesComparisonToHashForm() {
        String sql = "SELECT a FROM t1";
        String hashLine = ResultHasher.hashForm(List.of("1", "2", "3"));
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor()
                .queryReturns(sql, List.of(List.of("1"), List.of("2"), List.of("3")));

        FileRunResult withThreshold = run(
                executor,
                hashThreshold(2),
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, null, sql, List.of(hashLine)));
        FileRunResult withoutThreshold = run(
                new FakeDatabaseExecutor()
                        .queryReturns(sql, List.of(List.of("1"), List.of("2"), List.of("3"))),
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, null, sql, List.of(hashLine)));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(withThreshold));
        assertEquals(List.of(RecordOutcome.FAILED), outcomes(withoutThreshold));
    }

    @Test
    void skippedHashThresholdRecordLeavesThresholdUnchanged() {
        String sql = "SELECT a FROM t1";
        String hashLine = ResultHasher.hashForm(List.of("1", "2", "3"));
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor()
                .queryReturns(sql, List.of(List.of("1"), List.of("2"), List.of("3")));

        FileRunResult result = run(
                executor,
                skipIf("sqlite"),
                hashThreshold(2),
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, null, sql, List.of(hashLine)));

        assertEquals(List.of(RecordOutcome.FAILED), outcomes(result));
    }

    @Test
    void queryComparesExpectedResultsThroughNormalize() {
        String sql = "SELECT a, b FROM t1";
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor()
                .queryReturns(sql, List.of(row("2", "x"), row("1", null)));

        FileRunResult passing = run(
                executor,
                query(
                        List.of(ColumnType.INTEGER, ColumnType.TEXT),
                        SortMode.ROWSORT,
                        null,
                        sql,
                        List.of("1", "NULL", "2", "x")));
        FileRunResult failing = run(
                new FakeDatabaseExecutor().queryReturns(sql, List.of(row("2", "x"), row("1", null))),
                query(
                        List.of(ColumnType.INTEGER, ColumnType.TEXT),
                        SortMode.ROWSORT,
                        null,
                        sql,
                        List.of("1", "NULL", "9", "x")));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(passing));
        assertEquals(List.of(RecordOutcome.FAILED), outcomes(failing));
        assertFalse(failing.recordResults().get(0).failureReason().isEmpty());
    }

    @Test
    void valuePerLineExpected_passesWithoutDeclaredSeparator() {
        String sql = "SELECT 1, 2, 3";
        FakeDatabaseExecutor executor =
                new FakeDatabaseExecutor().queryReturns(sql, List.of(row("1", "2", "3")));

        FileRunResult result = run(
                executor,
                query(
                        List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                        SortMode.NOSORT,
                        null,
                        sql,
                        List.of("1", "2", "3")));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(result));
    }

    @Test
    void queryRecordDeclaredPipeSeparator_passesRowWise() {
        String sql = "SELECT 1, 2, 3";
        FakeDatabaseExecutor executor =
                new FakeDatabaseExecutor().queryReturns(sql, List.of(row("1", "2", "3")));

        FileRunResult result = run(
                executor,
                query(
                        List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                        SortMode.NOSORT,
                        null,
                        sql,
                        List.of("1 | 2 | 3"),
                        Optional.of("|")));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(result));
    }

    @Test
    void p0_3_perQuerySeparatorScope_secondIsValuePerLine() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor()
                .queryReturns("SELECT pipe", List.of(row("1", "2", "3")))
                .queryReturns("SELECT space", List.of(row("4", "5", "6")));

        FileRunResult result = run(
                executor,
                query(
                        List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                        SortMode.NOSORT,
                        null,
                        "SELECT pipe",
                        List.of("1|2|3"),
                        Optional.of("|")),
                query(
                        List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                        SortMode.NOSORT,
                        null,
                        "SELECT space",
                        List.of("4", "5", "6")));

        assertEquals(List.of(RecordOutcome.PASSED, RecordOutcome.PASSED), outcomes(result));
    }

    @Test
    void queryWithoutExpectedResultsOnlyAssertsExecution() {
        String sql = "SELECT a FROM t1";
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor().queryReturns(sql, List.of(List.of("7")));

        FileRunResult passing = run(executor, executeOnlyQuery(sql));
        FileRunResult failing = run(
                new FakeDatabaseExecutor().queryFails(sql, "no such table: t1"), executeOnlyQuery(sql));

        assertEquals(List.of(RecordOutcome.PASSED), outcomes(passing));
        assertEquals(List.of(RecordOutcome.FAILED), outcomes(failing));
        assertTrue(failing.recordResults().get(0).failureReason().contains("no such table: t1"));
    }

    @Test
    void queryExecutionFailureFailsRecordWithoutComparing() {
        String sql = "SELECT a FROM t1";
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor().queryFails(sql, "no such column: a");

        FileRunResult result = run(
                executor, query(List.of(ColumnType.INTEGER), SortMode.NOSORT, null, sql, List.of("1")));

        assertEquals(List.of(RecordOutcome.FAILED), outcomes(result));
        assertTrue(result.recordResults().get(0).failureReason().contains("no such column: a"));
    }

    @Test
    void conflictingResultsForSameLabelFailLaterRecord() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor()
                .queryReturns("SELECT 1", List.of(List.of("1")))
                .queryReturns("SELECT 2", List.of(List.of("2")));

        FileRunResult result = run(
                executor,
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, "l1", "SELECT 1", List.of("1")),
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, "l1", "SELECT 2", List.of("2")));

        assertEquals(List.of(RecordOutcome.PASSED, RecordOutcome.FAILED), outcomes(result));
        assertTrue(result.recordResults().get(1).failureReason().contains("label"));
        assertTrue(result.recordResults().get(1).failureReason().contains("l1"));
    }

    @Test
    void matchingResultsForSameLabelPass() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor()
                .queryReturns("SELECT 1", List.of(List.of("1")))
                .queryReturns("SELECT 1 AS again", List.of(List.of("1")));

        FileRunResult result = run(
                executor,
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, "l1", "SELECT 1", List.of("1")),
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, "l1", "SELECT 1 AS again", List.of("1")));

        assertEquals(List.of(RecordOutcome.PASSED, RecordOutcome.PASSED), outcomes(result));
    }

    @Test
    void labelConsistencyComparesHashFormWhenThresholdApplies() {
        String hashLine = ResultHasher.hashForm(List.of("1", "2", "3"));
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor()
                .queryReturns("SELECT wide", List.of(List.of("1"), List.of("2"), List.of("3")))
                .queryReturns("SELECT narrow", List.of(List.of("1")));

        FileRunResult result = run(
                executor,
                hashThreshold(2),
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, "l1", "SELECT wide", List.of(hashLine)),
                query(List.of(ColumnType.INTEGER), SortMode.NOSORT, "l1", "SELECT narrow", List.of("1")));

        assertEquals(List.of(RecordOutcome.PASSED, RecordOutcome.FAILED), outcomes(result));
        assertTrue(result.recordResults().get(1).failureReason().contains("label"));
    }

    @Test
    void fatalDatabaseFailureAbortsFileAndKeepsEarlierResults() {
        FakeDatabaseExecutor executor = new FakeDatabaseExecutor().fatalOn("SELECT 'dies'");

        FileRunResult result = run(
                executor,
                statementOk("SELECT 'ok'"),
                statementOk("SELECT 'dies'"),
                statementOk("SELECT 'never runs'"));

        assertEquals(List.of(RecordOutcome.PASSED, RecordOutcome.FAILED), outcomes(result));
        assertTrue(result.aborted());
        assertFalse(result.abortReason().isEmpty());
        assertEquals(List.of("SELECT 'ok'", "SELECT 'dies'"), executor.executedSql());
    }

    @Test
    void recordResultsCarrySourceLocation() {
        StatementRecord record = statementOk("SELECT 1");

        FileRunResult result = run(new FakeDatabaseExecutor(), record);

        assertEquals(record.location(), result.recordResults().get(0).location());
        assertEquals(SOURCE, result.recordResults().get(0).location().sourceName());
    }

    private FileRunResult run(FakeDatabaseExecutor executor, SqlTestRecord... records) {
        return new SqlLogicTestRunner(executor).run(List.of(records));
    }

    private static List<RecordOutcome> outcomes(FileRunResult result) {
        return result.recordResults().stream().map(RecordResult::outcome).toList();
    }

    /** Builds a raw result row; {@code null} values stand for SQL NULL. */
    private static List<String> row(String... values) {
        return Arrays.asList(values);
    }

    private StatementRecord statementOk(String sql) {
        return new StatementRecord(sql, StatementExpectation.OK, location());
    }

    private StatementRecord statementError(String sql) {
        return new StatementRecord(sql, StatementExpectation.ERROR, location());
    }

    private QueryRecord query(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            String label,
            String sql,
            List<String> expectedResults) {
        return query(typeSignature, sortMode, label, sql, expectedResults, Optional.empty());
    }

    private QueryRecord query(
            List<ColumnType> typeSignature,
            SortMode sortMode,
            String label,
            String sql,
            List<String> expectedResults,
            Optional<String> columnSeparator) {
        return new QueryRecord(
                typeSignature,
                sortMode,
                Optional.ofNullable(label),
                sql,
                true,
                expectedResults,
                columnSeparator,
                location());
    }

    private QueryRecord executeOnlyQuery(String sql) {
        return new QueryRecord(
                List.of(ColumnType.INTEGER),
                SortMode.NOSORT,
                Optional.empty(),
                sql,
                false,
                List.of(),
                Optional.empty(),
                location());
    }

    private SkipIfRecord skipIf(String dbName) {
        return new SkipIfRecord(dbName, location());
    }

    private OnlyIfRecord onlyIf(String dbName) {
        return new OnlyIfRecord(dbName, location());
    }

    private HashThresholdRecord hashThreshold(int threshold) {
        return new HashThresholdRecord(threshold, location());
    }

    private HaltRecord halt() {
        return new HaltRecord(location());
    }

    private SourceLocation location() {
        return new SourceLocation(SOURCE, nextLine++);
    }
}
