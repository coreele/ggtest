package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Spec acceptance for the human-readable CLI report (P0-1…P0-3, P1-1…P1-5).
 */
class CliReportAcceptanceTest {

    private static final Pattern STATUS_PASSED =
            Pattern.compile("\\.\\. \\[PASSED\\] in \\d+ ms");
    private static final Pattern STATUS_FAILED =
            Pattern.compile("\\.\\. \\[FAILED\\] in \\d+ ms");
    private static final Pattern STATUS_SKIPPED =
            Pattern.compile("\\.\\. \\[SKIPPED\\](?! in)");
    private static final Pattern ANSI = Pattern.compile("\\u001B\\[[0-9;]*m");

    @TempDir
    Path tempDir;

    @Test
    void p0_1_successFileCountAndExitZero() {
        Capture capture = run("--url", "jdbc:sqlite::memory:", fixture("pass.test").toString());

        assertEquals(0, capture.exitCode(), capture::dump);
        assertFalse(capture.stdout().contains("FILE:"));
        assertFalse(capture.stdout().contains("[OK]"));
        assertFalse(capture.stdout().contains("PASS in"));
        assertTrue(STATUS_PASSED.matcher(stripAnsi(capture.stdout())).find(), capture::dump);
        assertEquals(1, totalPassed(capture.stdout()));
        assertEquals(0, totalFailed(capture.stdout()));
        assertEquals(0, totalSkipped(capture.stdout()));
        assertFalse(ANSI.matcher(capture.stdout()).find(), "auto+non-TTY must be plain");
    }

    @Test
    void p0_2_queryMismatchShowsWhySqlDiffAt() {
        Capture capture = run("--url", "jdbc:sqlite::memory:", fixture("fail.test").toString());

        assertEquals(1, capture.exitCode(), capture::dump);
        String out = stripAnsi(capture.stdout());
        assertTrue(STATUS_FAILED.matcher(out).find(), out);
        assertFalse(out.contains(" after "));
        assertTrue(out.contains("[WHY]"));
        assertTrue(out.contains("[SQL] SELECT name ..."), out);
        assertFalse(out.matches("(?s).*\\[SQL\\] SELECT name FROM items.*"), out);
        assertTrue(out.contains("[Diff] (-expected|+actual)"));
        assertTrue(out.contains("at ") && out.contains("fail.test:"));
        assertTrue(out.contains("Error: some test case failed:"));
        assertTrue(out.contains("\"") && out.contains("fail.test"));
        assertFalse(out.contains("reason="));
        assertEquals(1, totalFailed(out));
        assertEquals(0, totalPassed(out));
    }

    @Test
    void p0_3_passwordNeverPrinted() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--password", "super-secret-credential",
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode());
        assertFalse(capture.stdout().contains("super-secret-credential"));
        assertFalse(capture.stderr().contains("super-secret-credential"));
    }

    @Test
    void p1_1_mixedOrderInlineDetailsErrorOnlyFailed() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("fail.test").toString(),
                fixture("pass.test").toString());

        assertEquals(1, capture.exitCode(), capture::dump);
        String out = stripAnsi(capture.stdout());
        int failIdx = out.indexOf("[FAILED]");
        int passIdx = out.indexOf("[PASSED]");
        assertTrue(failIdx >= 0 && passIdx > failIdx, out);
        assertTrue(out.indexOf("[WHY]") > failIdx && out.indexOf("[WHY]") < passIdx, out);

        int errorIdx = out.indexOf("Error: some test case failed:");
        assertTrue(errorIdx > passIdx, out);
        String errorBlock = out.substring(errorIdx, out.indexOf("TOTAL:"));
        assertTrue(errorBlock.contains("fail.test"));
        assertFalse(errorBlock.contains("pass.test"));

        assertEquals(1, totalPassed(out));
        assertEquals(1, totalFailed(out));

        // Success/skip lines must not insert an extra blank block between them;
        // failed→passed may have blank after the failure block only.
        assertFalse(out.contains("PASS in"));
        assertFalse(out.contains(" after "));
    }

    @Test
    void p1_2_statementFailureUsesFailedIn() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("statement-fail.test").toString());

        assertEquals(1, capture.exitCode(), capture::dump);
        String out = stripAnsi(capture.stdout());
        assertTrue(STATUS_FAILED.matcher(out).find(), out);
        assertTrue(out.contains("[WHY]"));
        String sqlLine = out.lines()
                .filter(line -> line.contains("[SQL]"))
                .findFirst()
                .orElse("");
        assertTrue(
                sqlLine.contains("[SQL] INSERT INTO definitely_missing_ggtest_table VALUES (1)"),
                out);
        assertFalse(sqlLine.endsWith(" ..."), "single-line SQL must not append ellipsis: " + sqlLine);
        assertTrue(out.contains("at ") && out.contains("statement-fail.test:"));
        assertEquals(1, totalFailed(out));
    }

    @Test
    void p1_3_hardErrorCountsFailedAndExitsTwo() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("bad-parse.test").toString(),
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode(), capture::dump);
        String out = stripAnsi(capture.stdout());
        assertTrue(out.contains("[FAILED]"), out);
        assertTrue(out.contains("[WHY]"), out);
        assertTrue(out.contains("bad-parse.test"), out);
        assertTrue(out.contains("[PASSED]"), out);
        assertEquals(1, totalFailed(out));
        assertEquals(1, totalPassed(out));
        assertFalse(out.contains("FILE:"));
    }

    @Test
    void skippedFileUsesSkippedTagWithoutTiming() {
        Capture capture = run("--url", "jdbc:sqlite::memory:", fixture("skip-all.test").toString());

        assertEquals(0, capture.exitCode(), capture::dump);
        String out = stripAnsi(capture.stdout());
        assertTrue(STATUS_SKIPPED.matcher(out).find(), out);
        assertFalse(out.matches("(?s).*\\[SKIPPED\\] in \\d+ ms.*"));
        assertEquals(1, totalSkipped(out));
        assertEquals(0, totalPassed(out));
        assertEquals(0, totalFailed(out));
    }

    @Test
    void statusLinePathColumnUsesMaxOfLongestPathAndSixty() {
        Capture capture = run("--url", "jdbc:sqlite::memory:", fixture("pass.test").toString());

        assertEquals(0, capture.exitCode(), capture::dump);
        String firstLine = stripAnsi(capture.stdout()).lines().findFirst().orElse("");
        int dots = firstLine.indexOf(" .. ");
        assertTrue(dots >= 0, firstLine);
        String pathColumn = firstLine.substring(0, dots);
        String path = pathColumn.stripTrailing();
        assertEquals(
                Math.max(path.length(), 60),
                pathColumn.length(),
                "path column = max(longest path, 60); line=" + firstLine);
        assertTrue(firstLine.contains(" .. [PASSED] in "), firstLine);
    }

    @Test
    void p1_4_colorAlwaysHasAnsiNeverHasNone() {
        Capture always = run(
                "--color", "always",
                "--url", "jdbc:sqlite::memory:",
                fixture("fail.test").toString());
        assertEquals(1, always.exitCode());
        assertTrue(ANSI.matcher(always.stdout()).find(), always::dump);

        Capture never = run(
                "--color", "never",
                "--url", "jdbc:sqlite::memory:",
                fixture("fail.test").toString());
        assertEquals(1, never.exitCode());
        assertFalse(ANSI.matcher(never.stdout()).find(), never::dump);
    }

    @Test
    void p1_5_colorPriorityPropertyOverEnvAndCliOverBoth() {
        Map<String, String> env = new HashMap<>();
        Map<String, String> props = new HashMap<>();

        env.put(RuntimeConfigResolver.COLOR_ENV, "never");
        Capture fromEnv = runWith(env, props,
                "--url", "jdbc:sqlite::memory:",
                fixture("fail.test").toString());
        assertEquals(1, fromEnv.exitCode());
        assertFalse(ANSI.matcher(fromEnv.stdout()).find(), fromEnv::dump);

        env.put(RuntimeConfigResolver.COLOR_ENV, "always");
        props.put(RuntimeConfigResolver.COLOR_PROPERTY, "never");
        Capture propertyWins = runWith(env, props,
                "--url", "jdbc:sqlite::memory:",
                fixture("fail.test").toString());
        assertEquals(1, propertyWins.exitCode());
        assertFalse(ANSI.matcher(propertyWins.stdout()).find(), propertyWins::dump);

        Capture cliWins = runWith(env, props,
                "--color", "always",
                "--url", "jdbc:sqlite::memory:",
                fixture("fail.test").toString());
        assertEquals(1, cliWins.exitCode());
        assertTrue(ANSI.matcher(cliWins.stdout()).find(), cliWins::dump);
    }

    @Test
    void usageErrorIsMultilineAndDoesNotPretendSuccess() {
        Capture capture = run(fixture("pass.test").toString());
        assertEquals(2, capture.exitCode());
        assertTrue(capture.stderr().contains("Error: usage"));
        assertTrue(capture.stderr().contains("[WHY]"));
        assertFalse(capture.stdout().contains("[PASSED]"));
        assertFalse(capture.stdout().contains("TOTAL:"));
        assertFalse(capture.stdout().contains("FILE:"));
    }

    private Capture run(String... args) {
        return runWith(Map.of(), Map.of(), args);
    }

    private Capture runWith(Map<String, String> env, Map<String, String> props, String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int code = Main.run(
                args,
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8),
                env::get,
                tempDir,
                props::get);
        return new Capture(code, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private static Path fixture(String relative) {
        try {
            URL url = CliReportAcceptanceTest.class.getResource("/fixtures/cli/" + relative);
            if (url == null) {
                throw new IllegalStateException("missing fixture: " + relative);
            }
            return Paths.get(url.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String stripAnsi(String text) {
        return ANSI.matcher(text).replaceAll("");
    }

    private static int totalPassed(String stdout) {
        return totalField(stdout, "passed");
    }

    private static int totalFailed(String stdout) {
        return totalField(stdout, "failed");
    }

    private static int totalSkipped(String stdout) {
        return totalField(stdout, "skipped");
    }

    private static int totalField(String stdout, String field) {
        Matcher matcher = Pattern.compile("TOTAL:.*" + field + "=(\\d+)", Pattern.CASE_INSENSITIVE)
                .matcher(stripAnsi(stdout));
        assertTrue(matcher.find(), "missing TOTAL " + field + " in:\n" + stdout);
        return Integer.parseInt(matcher.group(1));
    }

    private record Capture(int exitCode, String stdout, String stderr) {
        String dump() {
            return "exit=" + exitCode + "\nstdout:\n" + stdout + "\nstderr:\n" + stderr;
        }
    }
}
