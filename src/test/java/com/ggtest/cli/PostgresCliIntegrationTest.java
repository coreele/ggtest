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

/**
 * P0-PG-2/3/4 CLI paths gated by {@code GGTEST_PG_URL} (optional user/password).
 */
class PostgresCliIntegrationTest {

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
        assertTrue(capture.stdout().toLowerCase().contains("skipped"));
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

    private static void assumePg() {
        assumeTrue(
                System.getenv("GGTEST_PG_URL") != null && !System.getenv("GGTEST_PG_URL").isBlank(),
                "GGTEST_PG_URL not set; skipping PG CLI tests");
    }

    private static Capture runPg(String engine, String... files) {
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
        for (String file : files) {
            args.add(file);
        }

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        int code = Main.run(
                args.toArray(String[]::new),
                new PrintStream(stdout, true, StandardCharsets.UTF_8),
                new PrintStream(stderr, true, StandardCharsets.UTF_8));
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
