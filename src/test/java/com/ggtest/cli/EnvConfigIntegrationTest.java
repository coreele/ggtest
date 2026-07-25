package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * P0-ENV / P1-ENV CLI integration using temporary {@code .env} files (never a repo {@code .env}).
 *
 * <p>Injects empty env lookup + {@link #tempDir} as working directory so repo-root
 * {@code .env} / process {@code GGTEST_*} cannot pollute assertions (DEF-PG-003).
 */
class EnvConfigIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void onlyEnvFileUrlRunsWithoutCliUrl() throws IOException {
        Path envFile = tempDir.resolve("runtime.env");
        Files.writeString(envFile, "GGTEST_URL=jdbc:sqlite::memory:\n");

        Capture capture = run(
                "--env-file", envFile.toString(),
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode(), () -> capture.stdout() + capture.stderr());
        assertTrue(capture.stdout().contains("pass.test"));
    }

    @Test
    void cliUrlOverridesEnvFileUrl() throws IOException {
        Path envFile = tempDir.resolve("runtime.env");
        Files.writeString(envFile, "GGTEST_URL=jdbc:sqlite:/this/path/definitely/does/not/exist/ggtest-env.db?mode=ro\n");

        Capture capture = run(
                "--env-file", envFile.toString(),
                "--url", "jdbc:sqlite::memory:",
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode(), () -> capture.stdout() + capture.stderr());
    }

    @Test
    void explicitMissingEnvFileExitsTwo() {
        Capture capture = run(
                "--env-file", tempDir.resolve("empty-missing.env").toString(),
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertTrue(capture.stderr().toLowerCase().contains("env"));
    }

    @Test
    void threeSourcesWithoutUrlExitsTwo() throws IOException {
        Path envFile = tempDir.resolve("no-url.env");
        Files.writeString(envFile, "GGTEST_ENGINE=sqlite\nUNKNOWN=x\n");

        Capture capture = run(
                "--env-file", envFile.toString(),
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertTrue(capture.stderr().toLowerCase().contains("url"));
        assertFalse(capture.stdout().contains("FILE:"));
    }

    @Test
    void passwordFromEnvFileNeverPrinted() throws IOException {
        Path envFile = tempDir.resolve("secret.env");
        Files.writeString(
                envFile,
                """
                GGTEST_URL=jdbc:sqlite::memory:
                GGTEST_PASSWORD=env-file-super-secret
                """);

        Capture capture = run(
                "--env-file", envFile.toString(),
                fixture("pass.test").toString());

        assertEquals(0, capture.exitCode());
        assertFalse(capture.stdout().contains("env-file-super-secret"));
        assertFalse(capture.stderr().contains("env-file-super-secret"));
    }

    @Test
    void unknownEngineExitsTwoWithoutRunning() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--engine", "mysql",
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertTrue(capture.stderr().toLowerCase().contains("engine"));
        assertFalse(capture.stdout().contains("FILE:"));
    }

    @Test
    void engineUrlMismatchExitsTwoWithoutRunning() {
        Capture capture = run(
                "--url", "jdbc:sqlite::memory:",
                "--engine", "postgres",
                fixture("pass.test").toString());

        assertEquals(2, capture.exitCode());
        assertFalse(capture.stdout().contains("FILE:"));
    }

    @Test
    void envExampleExistsWithPlaceholderKeys() throws IOException {
        Path example = Path.of(".env.example");
        assertTrue(Files.isRegularFile(example), ".env.example must exist");
        String body = Files.readString(example);
        assertTrue(body.contains("GGTEST_URL"));
        assertTrue(body.contains("GGTEST_USER"));
        assertTrue(body.contains("GGTEST_PASSWORD"));
        assertTrue(body.contains("GGTEST_ENGINE"));
        assertTrue(body.contains("GGTEST_HASH_THRESHOLD"));
        assertFalse(body.toLowerCase().contains("production-password"));
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
            URL url = EnvConfigIntegrationTest.class.getResource("/fixtures/cli/" + relative);
            if (url == null) {
                throw new IllegalStateException("missing fixture: " + relative);
            }
            return Paths.get(url.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private record Capture(int exitCode, String stdout, String stderr) {}
}
