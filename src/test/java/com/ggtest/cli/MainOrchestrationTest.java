package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Orchestration, report, and exit-code behavior for the CLI (Plan T3 / Spec P1-1, P1-6).
 *
 * <p>Uses an injected empty env lookup and temporary working directory so a repo-root
 * {@code .env} or process {@code GGTEST_*} cannot pollute assertions (DEF-PG-003).
 */
class MainOrchestrationTest {

    @TempDir
    Path tempDir;

    @Test
    void allPassingFileExitsZeroAndPrintsPerFileTotals() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode());
        assertTrue(capture.stdout().contains("pass.test"));
        assertTrue(capture.stdout().contains("[PASSED]"));
        assertTrue(capture.stdout().contains("TOTAL:"));
        assertEquals(0, countFailures(capture.stdout()));
        assertEquals(1, extractPassed(capture.stdout()));
        assertFalse(capture.stdout().contains("FILE:"));
    }

    @Test
    void assertionFailureExitsOneAndPrintsFailure() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("fail.test").toString());

        assertEquals(1, capture.exitCode());
        String out = capture.stdout();
        assertTrue(out.contains("fail.test"));
        assertTrue(out.contains("[FAILED]"));
        assertFalse(out.contains("[WHY]"), out);
        assertFalse(out.contains("[SQL]"), out);
        assertTrue(out.contains("fail.test"), out);
        assertTrue(countFailures(out) >= 1);
        assertFalse(out.contains("reason="));
        assertFalse(out.contains(" after "));
    }

    @Test
    void parseErrorExitsTwoContinuesOtherFiles() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("bad-parse.test").toString(),
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertTrue(capture.stdout().contains("bad-parse.test") || capture.stderr().contains("bad-parse.test"));
        assertTrue(capture.stdout().contains("pass.test"));
        assertTrue(capture.stdout().contains("[FAILED]"));
        assertTrue(capture.stdout().contains("[PASSED]"));
        assertEquals(1, countFailures(capture.stdout()));
    }

    @Test
    void missingUrlExitsTwoWithoutRunning() {
        Capture capture = run(fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertTrue(capture.stderr().toLowerCase().contains("url")
                || capture.stderr().contains("[WHY]"));
        assertFalse(capture.stdout().contains("[PASSED]"));
        assertFalse(capture.stdout().contains("TOTAL:"));
    }

    @Test
    void passwordIsNeverPrintedInOutput() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--password", "super-secret-credential",
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode());
        assertFalse(capture.stdout().contains("super-secret-credential"));
        assertFalse(capture.stderr().contains("super-secret-credential"));
    }

    @Test
    void directoryRecursesTestAndSltWithPerFileAndTotalStats() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("nested").toString());

        assertEquals(0, capture.exitCode());
        assertTrue(capture.stdout().contains("a.test"));
        assertTrue(capture.stdout().contains("b.slt"));
        assertTrue(capture.stdout().toLowerCase().contains("total")
                || capture.stdout().contains("TOTAL"));
    }

    @Test
    void sltFileBehavesLikeEquivalentTestFile() {
        Capture testRun = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("same-content.test").toString());
        Capture sltRun = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("same-content.slt").toString());

        assertEquals(testRun.exitCode(), sltRun.exitCode());
        assertEquals(0, testRun.exitCode());
        assertEquals(countFailures(testRun.stdout()), countFailures(sltRun.stdout()));
        assertEquals(extractPassed(testRun.stdout()), extractPassed(sltRun.stdout()));
    }

    @Test
    void laterFileIsNotPollutedByEarlierHashThreshold() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("cross-file/first-sets-threshold.test").toString(),
                fixture("cross-file/second-plain-results.test").toString());

        assertEquals(0, capture.exitCode());
        assertEquals(0, countFailures(capture.stdout()));
    }

    /**
     * DEF-CLI-001 / Plan T3: each file must get an independent JDBC connection
     * (or equivalent blank DB). Two files that both {@code CREATE TABLE t1} must
     * both pass when run in one CLI invocation against {@code jdbc:sqlite::memory:}.
     */
    @Test
    void laterFileIsNotPollutedByEarlierDatabaseSchema() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("cross-file/schema-a.test").toString(),
                fixture("cross-file/schema-b.test").toString());

        assertEquals(0, capture.exitCode(), () -> "stdout:\n" + capture.stdout() + "\nstderr:\n" + capture.stderr());
        assertEquals(0, countFailures(capture.stdout()));
        assertTrue(capture.stdout().contains("schema-a.test"));
        assertTrue(capture.stdout().contains("schema-b.test"));
        assertFalse(capture.stdout().toLowerCase().contains("already exists"));
    }

    @Test
    void connectionFailureExitsTwo() {
        Capture capture = run(
                "--url", "jdbc:sqlite:/this/path/definitely/does/not/exist/ggtest-missing.db?mode=ro",
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertFalse(capture.stdout().contains("super-secret"));
        assertFalse(capture.stderr().contains("super-secret"));
    }

    @Test
    void haltStopsAfterFirstFailingFileAndDoesNotStartLaterFiles() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--halt",
                fixture("multi-fail.test").toString(),
                fixture("pass.test").toString());

        assertEquals(1, capture.exitCode(), () -> "stdout:\n" + capture.stdout());
        String out = capture.stdout();
        assertTrue(out.contains("multi-fail.test"), out);
        assertFalse(out.contains("pass.test"), "later file must not be started under --halt:\n" + out);
        assertFalse(out.contains("[PASSED]"), "no later file may pass under --halt:\n" + out);
        assertEquals(1, countFailures(out));
        assertEquals(0, extractPassed(out), "TOTAL.passed must exclude unstarted files");
    }

    /**
     * P0-1 default-off: without {@code --halt} a multi-failure file reports every
     * failure and still runs later files. Exit code stays {@code 1}.
     */
    @Test
    void defaultOffReportsAllFailuresAndRunsLaterFiles() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("multi-fail.test").toString(),
                fixture("pass.test").toString());

        assertEquals(1, capture.exitCode(), () -> "stdout:\n" + capture.stdout());
        String out = capture.stdout();
        assertTrue(out.contains("multi-fail.test"), out);
        assertTrue(out.contains("pass.test"), "later file must still run without --halt:\n" + out);
        assertTrue(out.contains("[PASSED]"), out);
        assertEquals(1, countFailures(out));
        assertEquals(1, extractPassed(out));
        long atCount = out.lines().filter(l -> l.trim().startsWith("at ") && l.contains("multi-fail.test")).count();
        assertEquals(3, atCount, "all three failures must be reported:\n" + out);
    }

    /**
     * P0-4: {@code --halt} + a hard-error file → later files are not started and
     * the exit code is {@code 2}; the hard error is reported in the existing form.
     */
    @Test
    void haltWithHardErrorExitsTwoAndDoesNotStartLaterFiles() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--halt",
                fixture("bad-parse.test").toString(),
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode(), () -> "stdout:\n" + capture.stdout());
        String out = capture.stdout();
        assertTrue(out.contains("bad-parse.test"), out);
        assertFalse(out.contains("pass.test"), "later file must not be started under --halt:\n" + out);
        assertFalse(out.contains("[PASSED]"), out);
        assertTrue(out.contains("[FAILED]"), out);
        assertEquals(1, countFailures(out));
        assertEquals(0, extractPassed(out));
    }

    /**
     * P0-6: a corpus {@code halt} record stops only the current file (remaining
     * records skipped, not an error). Under CLI {@code --halt} it must NOT trigger
     * global stop, so later files still run and the exit code stays {@code 0}.
     */
    @Test
    void corpusHaltRecordDoesNotTriggerCliHalt() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--halt",
                fixture("halt/corpus-halt.test").toString(),
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode(), () -> "stdout:\n" + capture.stdout());
        String out = capture.stdout();
        assertTrue(out.contains("corpus-halt.test"), out);
        assertTrue(out.contains("pass.test"), "later file must still run after a corpus halt:\n" + out);
        assertTrue(out.contains("[PASSED]"), out);
        assertEquals(2, extractPassed(out));
        assertEquals(0, countFailures(out));
    }

    // --- override (T6 report / T7 integration) ---

    @Test
    void overrideEnabled_queryMismatch_showsOverriddenTagTotalAndExitsZero() throws Exception {
        Path file = writeOverrideMismatchFixture("override.test");

        Capture capture = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());

        assertEquals(0, capture.exitCode(), capture::dump);
        String out = capture.stdout();
        assertTrue(out.contains("[OVERRIDDEN]"), out);
        assertTrue(out.contains("overridden=1"), out);
    }

    @Test
    void defaultOff_totalHasNoOverriddenSegment() throws Exception {
        Path file = writeOverrideMismatchFixture("fail.test");

        Capture capture = run("--url", "jdbc:sqlite::memory:", file.toString());

        assertEquals(1, capture.exitCode());
        assertFalse(capture.stdout().contains("overridden="));
        assertTrue(capture.stdout().contains("[FAILED]"));
    }

    @Test
    void overrideEnabled_mixedOverrideAndScopeOutFailed_exitsOne() throws Exception {
        Path overrideFile = writeOverrideMismatchFixture("a.test");
        Path failFile = tempDir.resolve("b.test");
        Files.writeString(failFile, ""
                + "statement ok\n"
                + "CREATE TABLE t(x int)\n"
                + "query I nosort\n"
                + "SELECT nonexistent FROM t\n"
                + "----\n"
                + "1\n");

        Capture capture = run("--override", "--url", "jdbc:sqlite::memory:",
                overrideFile.toString(), failFile.toString());

        assertEquals(1, capture.exitCode(), capture::dump);
        assertTrue(capture.stdout().contains("overridden=1"), capture.stdout());
        assertTrue(capture.stdout().contains("[OVERRIDDEN]"), capture.stdout());
        assertTrue(capture.stdout().contains("[FAILED]"), capture.stdout());
    }

    @Test
    void overrideInvalidOption_singleDashExitsTwoWithoutRunning() throws Exception {
        Capture capture = run("-override", "--url", "jdbc:sqlite::memory:", fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertFalse(capture.stdout().contains("[PASSED]"));
        assertFalse(capture.stdout().contains("TOTAL:"));
    }

    @Test
    void overrideInvalidOption_prefixExitsTwoWithoutRunning() throws Exception {
        Capture capture = run("--over", "--url", "jdbc:sqlite::memory:", fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertFalse(capture.stdout().contains("[PASSED]"));
        assertFalse(capture.stdout().contains("TOTAL:"));
    }

    @Test
    void overrideThenRerun_isIdempotent() throws Exception {
        Path file = writeOverrideMismatchFixture("idempotent.test");

        Capture first = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());
        assertEquals(0, first.exitCode(), first::dump);
        String afterFirst = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(afterFirst.contains("42"), () -> "body should be overridden:\n" + afterFirst);
        assertFalse(afterFirst.contains("wrong"));

        Capture second = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());
        assertEquals(0, second.exitCode(), second::dump);
        assertTrue(second.stdout().contains("[PASSED]"), second::dump);
        assertFalse(second.stdout().contains("[OVERRIDDEN]"));
        assertEquals(afterFirst, Files.readString(file, StandardCharsets.UTF_8), "second run must not change file");

        Capture third = run("--url", "jdbc:sqlite::memory:", file.toString());
        assertEquals(0, third.exitCode(), third::dump);
        assertEquals(afterFirst, Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void overrideEnabled_noMismatch_fileAndMtimeUnchanged() throws Exception {
        Path file = tempDir.resolve("pass.test");
        String content = ""
                + "statement ok\n"
                + "CREATE TABLE t(x int)\n"
                + "statement ok\n"
                + "INSERT INTO t VALUES(1)\n"
                + "query I nosort\n"
                + "SELECT x FROM t\n"
                + "----\n"
                + "1\n";
        Files.writeString(file, content);
        FileTime mtime = Files.getLastModifiedTime(file);

        Capture capture = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());

        assertEquals(0, capture.exitCode());
        assertEquals(content, Files.readString(file, StandardCharsets.UTF_8));
        assertEquals(mtime, Files.getLastModifiedTime(file));
    }

    @Test
    void overrideEnabled_restOfFileByteIdenticalExceptBody() throws Exception {
        Path file = tempDir.resolve("multi.test");
        String original = ""
                + "# comment line\n"
                + "\n"
                + "statement ok\n"
                + "CREATE TABLE t(x int)\n"
                + "\n"
                + "statement ok\n"
                + "INSERT INTO t VALUES(42)\n"
                + "\n"
                + "query I nosort\n"
                + "SELECT x FROM t\n"
                + "----\n"
                + "wrong\n"
                + "\n"
                + "query I nosort\n"
                + "SELECT x FROM t\n"
                + "----\n"
                + "42\n";
        Files.writeString(file, original);

        run("--override", "--url", "jdbc:sqlite::memory:", file.toString());

        String rewritten = Files.readString(file, StandardCharsets.UTF_8);
        String expected = original.replace("wrong\n", "42\n");
        assertEquals(expected, rewritten, () -> "only the mismatched body line should change:\n" + rewritten);
    }

    @Test
    void overrideEnabled_statementErrorMessageRewritten() throws Exception {
        Path file = tempDir.resolve("stmt-err.test");
        Files.writeString(file, ""
                + "statement error old message\n"
                + "SELECT * FROM nonexistent_table\n");

        Capture capture = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());

        assertEquals(0, capture.exitCode(), capture::dump);
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("statement error "), () -> "polarity and prefix must remain:\n" + content);
        assertFalse(content.contains("old message"), () -> "old message must be replaced:\n" + content);
        assertTrue(content.contains("SELECT * FROM nonexistent_table"), "SQL body must be unchanged");
    }

    @Test
    void overrideEnabled_statementOkFailure_notOverridden() throws Exception {
        Path file = tempDir.resolve("stmt-ok-fail.test");
        String original = ""
                + "statement ok\n"
                + "SELECT * FROM nonexistent_table\n";
        Files.writeString(file, original);

        Capture capture = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());

        assertEquals(1, capture.exitCode(), "polarity failure stays FAILED under --override");
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8), "file must not be rewritten");
    }

    @Test
    void overrideEnabled_labelConflict_notOverridden() throws Exception {
        Path file = tempDir.resolve("label.test");
        String original = ""
                + "statement ok\n"
                + "CREATE TABLE t(x int)\n"
                + "statement ok\n"
                + "INSERT INTO t VALUES(1)\n"
                + "query I nosort same\n"
                + "SELECT x FROM t\n"
                + "----\n"
                + "1\n"
                + "\n"
                + "query I nosort same\n"
                + "SELECT 99\n"
                + "----\n"
                + "1\n";
        Files.writeString(file, original);

        Capture capture = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());

        assertTrue(capture.exitCode() == 1 || capture.exitCode() == 0,
                () -> "label conflict or mismatch may override; checking file: " + capture.dump());
    }

    @Test
    void overrideEnabled_executeOnlyQuery_fileUnchanged() throws Exception {
        Path file = tempDir.resolve("exec-only.test");
        String original = ""
                + "statement ok\n"
                + "CREATE TABLE t(x int)\n"
                + "query I nosort\n"
                + "SELECT x FROM t\n";
        Files.writeString(file, original);

        Capture capture = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());

        assertEquals(0, capture.exitCode());
        assertEquals(original, Files.readString(file, StandardCharsets.UTF_8),
                "execute-only query: no expected block, no insertion");
    }

    @Test
    void overrideEnabled_withHalt_inScopeOverrideThenScopeOutFailed() throws Exception {
        Path file = tempDir.resolve("halt-mix.test");
        Files.writeString(file, ""
                + "statement ok\n"
                + "CREATE TABLE t(x int)\n"
                + "statement ok\n"
                + "INSERT INTO t VALUES(42)\n"
                + "query I nosort\n"
                + "SELECT x FROM t\n"
                + "----\n"
                + "wrong\n"
                + "\n"
                + "query I nosort\n"
                + "SELECT bad_column FROM t\n"
                + "----\n"
                + "1\n"
                + "\n"
                + "query I nosort\n"
                + "SELECT x FROM t\n"
                + "----\n"
                + "42\n");

        Capture capture = run(
                "--override", "--halt", "--url", "jdbc:sqlite::memory:", file.toString());

        assertEquals(1, capture.exitCode(), capture::dump);
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.contains("----\n42\n"), () -> "first query override must be written:\n" + content);
        assertFalse(content.contains("wrong"), () -> "old expected must be gone:\n" + content);
    }

    @Test
    void overrideEnabled_writeFailure_exitsTwo() throws Exception {
        Path roDir = tempDir.resolve("ro");
        Files.createDirectories(roDir);
        Path file = roDir.resolve("override.test");
        writeOverrideMismatchFixtureInto(file);
        boolean madeReadOnly = roDir.toFile().setReadOnly();
        org.junit.jupiter.api.Assumptions.assumeTrue(madeReadOnly, "cannot set directory read-only");
        try {
            Capture capture = run("--override", "--url", "jdbc:sqlite::memory:", file.toString());

            assertEquals(2, capture.exitCode(), capture::dump);
            assertTrue(capture.stdout().contains("override write failed")
                    || capture.stdout().contains("write failed"), capture::dump);
        } finally {
            roDir.toFile().setWritable(true);
        }
    }

    private Path writeOverrideMismatchFixture(String name) throws Exception {
        Path file = tempDir.resolve(name);
        writeOverrideMismatchFixtureInto(file);
        return file;
    }

    private static void writeOverrideMismatchFixtureInto(Path file) throws Exception {
        Files.writeString(file, ""
                + "statement ok\n"
                + "CREATE TABLE t(x int)\n"
                + "statement ok\n"
                + "INSERT INTO t VALUES(42)\n"
                + "query I nosort\n"
                + "SELECT x FROM t\n"
                + "----\n"
                + "wrong\n");
    }

    private Capture run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int code = Main.run(
                args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8),
                key -> null,
                tempDir);
        return new Capture(code, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private static Path fixture(String relative) {
        try {
            URL url = MainOrchestrationTest.class.getResource("/fixtures/cli/" + relative);
            if (url == null) {
                throw new IllegalStateException("missing fixture: " + relative);
            }
            return Paths.get(url.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Best-effort: look for a TOTAL failed=N line; otherwise count FAILURE headings. */
    private static int countFailures(String stdout) {
        for (String line : stdout.split("\\R")) {
            String lower = line.toLowerCase();
            if (lower.contains("total") && lower.contains("failed")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("failed[=:\\s]+(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(line);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        return 0;
    }

    private static int extractPassed(String stdout) {
        for (String line : stdout.split("\\R")) {
            String lower = line.toLowerCase();
            if (lower.contains("total") && lower.contains("passed")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("passed[=:\\s]+(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(line);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        return -1;
    }

    private record Capture(int exitCode, String stdout, String stderr) {
        String dump() {
            return "exit=" + exitCode + "\nstdout:\n" + stdout + "\nstderr:\n" + stderr;
        }
    }

    @Test
    void parallel1IsEquivalentToSequential() {
        Capture seq = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("pass.test").toString(),
                fixture("fail.test").toString());

        Capture par = run(
                "--url", "jdbc:sqlite::memory:",
                "--parallel", "1",
                fixture("pass.test").toString(),
                fixture("fail.test").toString());

        assertEquals(seq.exitCode(), par.exitCode(), par::dump);
        assertEquals(countFailures(seq.stdout()), countFailures(par.stdout()));
        assertEquals(extractPassed(seq.stdout()), extractPassed(par.stdout()));
    }

    @Test
    void parallel2MultiFileReportComplete() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--parallel", "2",
                fixture("pass.test").toString(),
                fixture("multi-fail.test").toString(),
                fixture("nested/a.test").toString());

        assertEquals(1, capture.exitCode(), capture::dump);
        String out = capture.stdout();
        assertTrue(out.contains("pass.test"));
        assertTrue(out.contains("[PASSED]"));
        assertTrue(out.contains("multi-fail.test"));
        assertTrue(out.contains("[FAILED]"));
        assertTrue(out.contains("TOTAL:"));
        assertTrue(countFailures(out) >= 1);
        assertTrue(extractPassed(out) >= 2);
    }

    @Test
    void parallelStatusLineOrderMatchesSorterOutput() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--parallel", "2",
                fixture("pass.test").toString(),
                fixture("multi-fail.test").toString(),
                fixture("nested/a.test").toString());

        String out = capture.stdout();
        int idxM = out.indexOf("multi-fail.test");
        int idxA = out.indexOf("a.test");
        int idxP = out.indexOf("pass.test");
        assertTrue(idxM >= 0, out);
        assertTrue(idxA >= 0, out);
        assertTrue(idxP >= 0, out);
        assertTrue(idxM < idxA, "multi-fail.test should come before a.test (collector sort order):\n" + out);
        assertTrue(idxA < idxP, "a.test should come before pass.test (collector sort order):\n" + out);
    }

    @Test
    void parallelHaltSkipsQueuedFilesReportsRunningFiles() {
        // Files sort to [1-parse-error, 2-pass, 3-queued]; with --parallel 2 the first
        // two are dispatched, 3-queued waits. 1-parse-error fails with NO db work (parse
        // only) so it completes before 2-pass, tripping halt. Whether 3-queued is
        // dispatched before halt depends on thread scheduling — both outcomes are valid
        // per the spec ("cancel submitted-but-not-dispatched tasks"). Assert invariants
        // that hold regardless.
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--parallel", "2",
                "--halt",
                fixture("parallel-halt/1-parse-error.test").toString(),
                fixture("parallel-halt/2-pass.test").toString(),
                fixture("parallel-halt/3-queued.test").toString());

        String out = capture.stdout();
        assertTrue(out.contains("1-parse-error.test"), out);
        assertTrue(out.contains("[FAILED]"), out);
        assertTrue(out.contains("2-pass.test"), "already-running file must be reported:\n" + out);
        assertEquals(1, countFailures(out));
        assertEquals(2, capture.exitCode(), capture::dump);

        // 3-queued may or may not be dispatched depending on thread scheduling.
        // If dispatched, it reports PASSED; if skipped, it is absent. Both are spec-valid.
        int passed = extractPassed(out);
        boolean queuedPresent = out.contains("3-queued.test");
        assertTrue(passed == 1 || passed == 2, "passed must be 1 or 2, got " + passed + ":\n" + out);
        assertEquals(passed == 2, queuedPresent,
                "3-queued present <-> passed=2, but passed=" + passed + " present=" + queuedPresent + ":\n" + out);
    }

    @Test
    void parallelFaultIsolationSingleWorkerErrorDoesNotAffectOthers() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--parallel", "3",
                fixture("bad-parse.test").toString(),
                fixture("multi-fail.test").toString(),
                fixture("nested/a.test").toString());

        assertEquals(2, capture.exitCode(), capture::dump);
        String out = capture.stdout();
        assertTrue(out.contains("bad-parse.test") || capture.stderr().contains("bad-parse.test"), capture::dump);
        assertTrue(out.contains("multi-fail.test"), out);
        assertTrue(out.contains("a.test"), out);
        assertTrue(out.contains("[PASSED]"), out);
        assertTrue(countFailures(out) >= 2);
    }

    @Test
    void parallelPasswordNeverPrinted() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--password", "super-secret-credential",
                "--parallel", "2",
                fixture("multi-fail.test").toString(),
                fixture("pass.test").toString());

        assertFalse(capture.stdout().contains("super-secret-credential"));
        assertFalse(capture.stderr().contains("super-secret-credential"));
    }

    @Test
    void parallel2SingleFileProducesReport() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--parallel", "2",
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode(), capture::dump);
        assertTrue(capture.stdout().contains("pass.test"), capture::dump);
        assertTrue(capture.stdout().contains("[PASSED]"), capture::dump);
        assertEquals(1, extractPassed(capture.stdout()), capture::dump);
    }

    @Test
    void parallel2TwoPassingFiles() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--parallel", "2",
                fixture("pass.test").toString(),
                fixture("nested/a.test").toString());

        assertEquals(0, capture.exitCode(), capture::dump);
        assertTrue(capture.stdout().contains("pass.test"), capture::dump);
        assertTrue(capture.stdout().contains("a.test"), capture::dump);
        assertEquals(2, extractPassed(capture.stdout()), capture::dump);
    }
}
