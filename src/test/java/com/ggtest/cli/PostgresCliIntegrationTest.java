package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * P0-PG-2/3/4 CLI paths gated by {@code GGTEST_PG_URL} (optional user/password).
 *
 * <p>Gate vars {@code GGTEST_PG_*} are read only to build CLI argv. {@code Main.run}
 * receives an empty env lookup + temporary working directory so repo-root
 * {@code .env} / process {@code GGTEST_*} cannot override those flags (DEF-PG-003).
 */
class PostgresCliIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void postgresEngineRunsBasicFixture() {
        assumePg();
        Capture capture = runPg("postgres", fixture("basic.test").toString());

        assertEquals(0, capture.exitCode(), () -> capture.stdout() + "\n" + capture.stderr());
        assertEquals(0, countFailures(capture.stdout()));
    }

    @Test
    void skipifAndOnlyIfRespectPostgresEngineCaseInsensitively() {
        assumePg();
        Capture capture = runPg("Postgres", fixture("conditions.test").toString());

        assertEquals(0, capture.exitCode(), () -> capture.stdout() + "\n" + capture.stderr());
        assertEquals(0, countFailures(capture.stdout()));
        assertTrue(capture.stdout().contains("[PASSED]") || capture.stdout().contains("[SKIPPED]"));
    }

    @Test
    void crossFileSchemaIsolationKeepsSameNamedTablesIndependent() {
        assumePg();
        Capture capture = runPg(
                "postgres",
                fixture("cross-file/schema-a.test").toString(),
                fixture("cross-file/schema-b.test").toString());

        assertEquals(0, capture.exitCode(), () -> capture.stdout() + "\n" + capture.stderr());
        assertEquals(0, countFailures(capture.stdout()));
        assertTrue(capture.stdout().contains("schema-a.test"));
        assertTrue(capture.stdout().contains("schema-b.test"));
        assertFalse(capture.stdout().toLowerCase().contains("already exists"));
    }

    @Test
    void passwordIsNeverPrintedWhenRunningPostgres() {
        assumePg();
        String password = System.getenv("GGTEST_PG_PASSWORD");
        assumeTrue(password != null && !password.isBlank(), "GGTEST_PG_PASSWORD not set");

        Capture capture = runPg("postgres", fixture("basic.test").toString());

        assertEquals(0, capture.exitCode(), () -> capture.stdout() + "\n" + capture.stderr());
        assertFalse(capture.stdout().contains(password));
        assertFalse(capture.stderr().contains(password));
    }

    /**
     * Controllable non-empty password path: synthetic CLI {@code --password} against an
     * unreachable PG URL. Proves connection assembly is attempted and the password never
     * appears on stdout/stderr (no real PG required). Connection hard-errors exit {@code 2}.
     */
    @Test
    void nonEmptyPasswordNeverPrintedWhenPostgresConnectionFails() {
        String password = "test-nonempty-pg-secret-never-echo";
        Capture capture = runMain(
                "--url",
                "jdbc:postgresql://127.0.0.1:1/ggtest_unreachable",
                "--engine",
                "postgres",
                "--user",
                "ggtest",
                "--password",
                password,
                fixture("basic.test").toString());

        String combined = capture.stdout() + "\n" + capture.stderr();
        assertTrue(
                combined.toLowerCase().contains("connection failed"),
                () -> "expected connection attempt path:\n" + combined);
        assertEquals(2, capture.exitCode(), () -> "connection hard-error exits 2:\n" + combined);
        assertFalse(capture.stdout().contains(password), "password must not appear on stdout");
        assertFalse(capture.stderr().contains(password), "password must not appear on stderr");
    }

    @Test
    void parallelPostgresSchemaIsolation() {
        assumePg();
        Capture capture = runPg(
                "postgres",
                "--parallel", "2",
                fixture("cross-file/schema-a.test").toString(),
                fixture("cross-file/schema-b.test").toString());

        assertEquals(0, capture.exitCode(), () -> capture.stdout() + "\n" + capture.stderr());
        assertEquals(0, countFailures(capture.stdout()));
        assertTrue(capture.stdout().contains("schema-a.test"));
        assertTrue(capture.stdout().contains("schema-b.test"));
        assertFalse(capture.stdout().toLowerCase().contains("already exists"));
        assertFalse(capture.stdout().toLowerCase().contains("conflict"));
        assertFalse(capture.stderr().toLowerCase().contains("conflict"));
    }

    private static void assumePg() {
        assumeTrue(
                System.getenv("GGTEST_PG_URL") != null && !System.getenv("GGTEST_PG_URL").isBlank(),
                "GGTEST_PG_URL not set; skipping PG CLI tests");
    }

    private Capture runPg(String engine, String... trailing) {
        List<String> args = new ArrayList<>();
        args.add("--url");
        args.add(System.getenv("GGTEST_PG_URL"));
        args.add("--engine");
        args.add(engine);
        String user = System.getenv("GGTEST_PG_USER");
        if (user != null && !user.isBlank()) {
            args.add("--user");
            args.add(user);
        }
        String password = System.getenv("GGTEST_PG_PASSWORD");
        if (password != null) {
            args.add("--password");
            args.add(password);
        }
        for (String item : trailing) {
            args.add(item);
        }
        return runMain(args.toArray(String[]::new));
    }

    private Capture runMain(String... args) {
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
            URL url = PostgresCliIntegrationTest.class.getResource("/fixtures/pg/" + relative);
            if (url == null) {
                throw new IllegalStateException("missing fixture: " + relative);
            }
            return Paths.get(url.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int countFailures(String stdout) {
        for (String line : stdout.split("\\R")) {
            String lower = line.toLowerCase();
            if (lower.contains("total") && lower.contains("failed")) {
                Matcher m = Pattern.compile("failed[=:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(line);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        return 0;
    }

    private record Capture(int exitCode, String stdout, String stderr) {}
}
