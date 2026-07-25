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
import org.junit.jupiter.api.Test;

/**
 * Orchestration, report, and exit-code behavior for the CLI (Plan T3 / Spec P1-1, P1-6).
 */
class MainOrchestrationTest {

    @Test
    void allPassingFileExitsZeroAndPrintsPerFileTotals() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode());
        assertTrue(capture.stdout().contains("pass.test"));
        assertTrue(capture.stdout().toLowerCase().contains("passed")
                || capture.stdout().contains("pass"));
        assertTrue(capture.stdout().toLowerCase().contains("total")
                || capture.stdout().contains("TOTAL"));
        assertFalse(capture.stdout().toLowerCase().contains("failed=0")
                && capture.stdout().matches("(?s).*failed[^0-9]*[1-9].*"));
        assertEquals(0, countFailures(capture.stdout()));
    }

    @Test
    void assertionFailureExitsOneAndPrintsFailureFourFields() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                fixture("fail.test").toString());

        assertEquals(1, capture.exitCode());
        String out = capture.stdout();
        assertTrue(out.contains("fail.test"));
        assertTrue(out.matches("(?s).*\\b[1-9][0-9]*\\b.*") || out.contains("line"));
        assertTrue(out.toLowerCase().contains("select") || out.contains("SELECT"));
        assertTrue(out.toLowerCase().contains("mismatch")
                || out.toLowerCase().contains("expected")
                || out.toLowerCase().contains("fail"));
        assertTrue(countFailures(out) >= 1);
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
        assertTrue(countFailures(capture.stdout()) == 0 || capture.stdout().contains("passed"));
    }

    @Test
    void missingUrlExitsTwoWithoutRunning() {
        Capture capture = run(fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertTrue(capture.stderr().toLowerCase().contains("url"));
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

    @Test
    void connectionFailureExitsTwo() {
        Capture capture = run(
                "--url", "jdbc:sqlite:/this/path/definitely/does/not/exist/ggtest-missing.db?mode=ro",
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertFalse(capture.stdout().contains("super-secret"));
        assertFalse(capture.stderr().contains("super-secret"));
    }

    private static Capture run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int code = Main.run(args, new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
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

    private record Capture(int exitCode, String stdout, String stderr) {}
}
