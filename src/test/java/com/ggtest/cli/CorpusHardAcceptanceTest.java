package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Optional L4 hard acceptance against a user-supplied official sqllogictest corpus
 * (P0-1 / P1-5). Enabled when {@code GGTEST_CORPUS_DIR} points at a directory that
 * contains {@code select1.test} (and for P1-5 also select2/select3). Never skips
 * with a pass — when the env var is unset the tests are assumed away and
 * {@code dev-notes.md} must record that hard acceptance was not executed.
 *
 * <p>Isolates process {@code GGTEST_*} and repo-root {@code .env} via injected
 * empty env lookup + temporary working directory (DEF-PG-003).
 */
class CorpusHardAcceptanceTest {

    @TempDir
    Path tempDir;

    private static Path corpusDir;

    @BeforeAll
    static void resolveCorpus() {
        String configured = System.getenv("GGTEST_CORPUS_DIR");
        if (configured == null || configured.isBlank()) {
            corpusDir = null;
            return;
        }
        Path dir = Path.of(configured).toAbsolutePath().normalize();
        assumeTrue(Files.isDirectory(dir), "GGTEST_CORPUS_DIR is not a directory: " + dir);
        corpusDir = dir;
    }

    @Test
    void p0_1_officialSelect1FailsZeroExitsZero() {
        assumeTrue(corpusDir != null, "GGTEST_CORPUS_DIR not set; hard acceptance not executed");
        Path select1 = corpusDir.resolve("select1.test");
        assumeTrue(Files.isRegularFile(select1), "select1.test missing under " + corpusDir);

        Capture capture = run("--url", "jdbc:sqlite::memory:", select1.toString());

        assertEquals(0, capture.exitCode());
        assertEquals(0, totalFailed(capture.stdout()));
        assertTrue(capture.stdout().contains("TOTAL:"));
    }

    @Test
    void p1_5_officialSelect123BatchFailsZeroExitsZero() {
        assumeTrue(corpusDir != null, "GGTEST_CORPUS_DIR not set; hard acceptance not executed");
        Path select1 = corpusDir.resolve("select1.test");
        Path select2 = corpusDir.resolve("select2.test");
        Path select3 = corpusDir.resolve("select3.test");
        assumeTrue(Files.isRegularFile(select1), "select1.test missing");
        assumeTrue(Files.isRegularFile(select2), "select2.test missing");
        assumeTrue(Files.isRegularFile(select3), "select3.test missing");

        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                select1.toString(),
                select2.toString(),
                select3.toString());

        assertEquals(0, capture.exitCode());
        assertEquals(0, totalFailed(capture.stdout()));
        assertTrue(capture.stdout().contains("select1.test"));
        assertTrue(capture.stdout().contains("select2.test"));
        assertTrue(capture.stdout().contains("select3.test"));
        assertTrue(capture.stdout().contains("TOTAL:"));
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

    private static int totalFailed(String stdout) {
        Pattern pattern = Pattern.compile("TOTAL:.*failed=(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(stdout);
        assertTrue(matcher.find(), "TOTAL failed= count missing in:\n" + stdout);
        return Integer.parseInt(matcher.group(1));
    }

    private record Capture(int exitCode, String stdout, String stderr) {}
}
