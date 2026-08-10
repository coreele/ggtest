package com.ggtest.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.db.sqlite.SqliteJdbcExecutor;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.parser.SqlLogicTestParser;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Spec acceptance for the runner-sqlite slice: parser → runner → SQLite JDBC on a
 * blank in-memory database (P0-3, P0-6, P1-2, P1-4), plus the real-database query
 * path. Dependency isolation (P0-8) is covered by
 * {@link RunnerDependencyIsolationTest} and by {@link #runnerDrivesAnyExecutorImplementation()}.
 */
class RunnerAcceptanceTest {

    private Connection connection;
    private SqlLogicTestRunner runner;

    @BeforeEach
    void openBlankDatabase() throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite::memory:");
        runner = new SqlLogicTestRunner(new SqliteJdbcExecutor(connection));
    }

    @AfterEach
    void closeDatabase() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    void p0_3_statementOkAndStatementErrorBothPass() {
        FileRunResult result = run("p0-3-statement-assertions.test");

        assertEquals(
                List.of(
                        RecordOutcome.PASSED,
                        RecordOutcome.PASSED,
                        RecordOutcome.PASSED,
                        RecordOutcome.PASSED),
                outcomes(result));
        assertEquals(0, result.failedCount());
    }

    @Test
    void p0_3_statementErrorWithValidSqlFails() {
        FileRunResult result = run("p0-3-statement-error-made-valid.test");

        assertEquals(
                List.of(
                        RecordOutcome.PASSED,
                        RecordOutcome.PASSED,
                        RecordOutcome.PASSED,
                        RecordOutcome.FAILED),
                outcomes(result));
        assertEquals(1, result.failedCount());
        assertFalse(result.recordResults().get(3).failureReason().isEmpty());
    }

    @Test
    void p0_6_conditionsSkipRecordsPerEngine() {
        FileRunResult result = run("p0-6-conditions.test");

        assertEquals(
                List.of(RecordOutcome.SKIPPED, RecordOutcome.PASSED, RecordOutcome.SKIPPED),
                outcomes(result));
        assertEquals(2, result.skippedCount());
        assertEquals(0, result.failedCount());
    }

    @Test
    void p1_2_recordsAfterHaltAreSkipped() {
        FileRunResult result = run("p1-2-halt.test");

        assertEquals(
                List.of(RecordOutcome.PASSED, RecordOutcome.SKIPPED, RecordOutcome.SKIPPED),
                outcomes(result));
        assertTrue(result.halted());
        assertEquals(0, result.failedCount());
        assertTrue(result.recordResults().get(1).failureReason().contains("halt"));
    }

    @Test
    void p1_4_conflictingLabelFailsSecondQuery() {
        FileRunResult result = run("p1-4-label-conflict.test");

        assertEquals(
                List.of(
                        RecordOutcome.PASSED,
                        RecordOutcome.PASSED,
                        RecordOutcome.PASSED,
                        RecordOutcome.FAILED),
                outcomes(result));
        String reason = result.recordResults().get(3).failureReason();
        assertTrue(reason.contains("label"), reason);
        assertTrue(reason.contains("samevals"), reason);
    }

    @Test
    void queryPathNormalizesSortsAndHashesRealResults() {
        FileRunResult result = run("query-normalize-smoke.test");

        assertEquals(0, result.failedCount(), () -> firstFailure(result));
        assertEquals(5, result.passedCount());
        assertFalse(result.aborted());
    }

    @Test
    void p0_9_valuePerLineSpacedTextFixturePasses() {
        FileRunResult result = run("value-per-line-spaced-text.test");

        assertEquals(0, result.failedCount(), () -> firstFailure(result));
        assertEquals(3, result.passedCount());
    }

    @Test
    void p0_2_p0_9_rowWisePipeBareTextQueryHeadFixturePasses() {
        FileRunResult result = run("rowwise-pipe-separator.test");

        assertEquals(0, result.failedCount(), () -> firstFailure(result));
        assertEquals(3, result.passedCount());
    }

    @Test
    void p0_3_p0_9_rowWiseMixedScopeAndHashFixturePasses() {
        FileRunResult result = run("rowwise-mixed.test");

        assertEquals(0, result.failedCount(), () -> firstFailure(result));
        assertEquals(6, result.passedCount());
    }

    @Test
    void p0_4_statementErrorWithOptionalMessageMatching() {
        FileRunResult result = run("p0-4-statement-error-message.test");

        assertEquals(
                List.of(
                        RecordOutcome.PASSED,  // statement ok CREATE TABLE
                        RecordOutcome.PASSED,  // statement ok INSERT
                        RecordOutcome.PASSED,  // statement error, no message: DROP TABLE missing fails
                        RecordOutcome.PASSED,  // statement error "no such table": INSERT INTO missing fails with matching message
                        RecordOutcome.FAILED,  // statement error "syntax error": fails but error is "no such table", mismatch
                        RecordOutcome.FAILED), // statement error "duplicate key": SELECT 1 succeeds, expected to fail
                outcomes(result));
        assertEquals(2, result.failedCount());
        assertEquals(4, result.passedCount());
        String reason4 = result.recordResults().get(4).failureReason();
        assertTrue(reason4.contains("message mismatch"), "record 4 must be a message mismatch: " + reason4);
        String reason5 = result.recordResults().get(5).failureReason();
        assertEquals("statement expected to fail but succeeded", reason5);
    }

    @Test
    void runnerDrivesAnyExecutorImplementation() {
        List<SqlTestRecord> records = parse("p0-6-conditions.test");
        FakeDatabaseExecutor otherEngine = new FakeDatabaseExecutor("duckdb");

        FileRunResult result = new SqlLogicTestRunner(otherEngine).run(records);

        assertEquals(
                List.of(RecordOutcome.PASSED, RecordOutcome.SKIPPED, RecordOutcome.SKIPPED),
                outcomes(result));
        assertEquals(List.of("INSERT INTO absent_when_skipped VALUES(1)"), otherEngine.executedSql());
    }

    private FileRunResult run(String fixture) {
        return runner.run(parse(fixture));
    }

    private static List<SqlTestRecord> parse(String fixture) {
        String resource = "/fixtures/runner/" + fixture;
        try (InputStream in = RunnerAcceptanceTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "missing fixture " + resource);
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return new SqlLogicTestParser().parse(fixture, content);
        } catch (IOException ex) {
            throw new IllegalStateException("cannot read " + resource, ex);
        }
    }

    private static List<RecordOutcome> outcomes(FileRunResult result) {
        return result.recordResults().stream().map(RecordResult::outcome).toList();
    }

    private static String firstFailure(FileRunResult result) {
        return result.recordResults().stream()
                .filter(r -> r.outcome() == RecordOutcome.FAILED)
                .map(r -> r.location() + " -> " + r.failureReason())
                .findFirst()
                .orElse("no failure");
    }
}
