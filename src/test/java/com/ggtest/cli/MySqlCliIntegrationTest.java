package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class MySqlCliIntegrationTest {

    @TempDir Path tempDir;

    private static void assumeMy() {
        assumeTrue(System.getenv("GGTEST_MY_URL") != null && !System.getenv("GGTEST_MY_URL").isBlank(),
                "GGTEST_MY_URL not set");
    }

    @Test void runsBasicFixture() { assumeMy(); var c = runMy(fixture("basic.test").toString()); assertEquals(0, c.exitCode, () -> c.stdout() + "\n" + c.stderr()); assertEquals(0, c.failures()); assertTrue(c.stdout.contains("[PASSED]")); }
    @Test void reportsAssertionFailure() { assumeMy(); var c = runMy(fixture("fail.test").toString()); assertEquals(1, c.exitCode, () -> c.stdout() + "\n" + c.stderr()); assertEquals(1, c.failures()); assertTrue(c.stdout.contains("[FAILED]")); }
    @Test void skipifOnlyif() { assumeMy(); var c = runMy(fixture("conditions.test").toString()); assertEquals(0, c.exitCode, () -> c.stdout() + "\n" + c.stderr()); assertEquals(0, c.failures()); }
    @Test void crossFileIsolation() { assumeMy(); var c = runMy(fixture("cross-file/schema-a.test").toString(), fixture("cross-file/schema-b.test").toString()); assertEquals(0, c.exitCode, () -> c.stdout() + "\n" + c.stderr()); assertEquals(0, c.failures()); }
    @Test void parallelIsolation() { assumeMy(); var c = runMy("--parallel", "2", fixture("cross-file/schema-a.test").toString(), fixture("cross-file/schema-b.test").toString()); assertEquals(0, c.exitCode, () -> c.stdout() + "\n" + c.stderr()); assertEquals(0, c.failures()); }
    @Test void haltStopsAfterFirstFailure() { assumeMy(); var c = runMy("--halt", fixture("fail.test").toString(), fixture("basic.test").toString()); assertEquals(1, c.exitCode, () -> c.stdout() + "\n" + c.stderr()); assertTrue(c.stdout.contains("[FAILED]")); }
    @Test void passwordNeverPrinted() { assumeMy(); var pw = System.getenv("GGTEST_MY_PASSWORD"); assumeTrue(pw != null && !pw.isBlank()); var c = runMy(fixture("basic.test").toString()); assertFalse(c.stdout.contains(pw)); assertFalse(c.stderr.contains(pw)); }

    @Test void nonEmptyPasswordNeverPrintedWhenConnectionFails() {
        String pw = "test-my-secret";
        var c = runMain("--url", "jdbc:mysql://127.0.0.1:1", "--engine", "mysql", "--user", "x", "--password", pw, fixture("basic.test").toString());
        assertTrue((c.stdout() + c.stderr()).toLowerCase().contains("connection failed"));
        assertEquals(2, c.exitCode);
        assertFalse(c.stdout.contains(pw));
        assertFalse(c.stderr.contains(pw));
    }

    private Capture runMy(String... trailing) {
        List<String> args = new ArrayList<>(List.of("--url", System.getenv("GGTEST_MY_URL"), "--engine", "mysql"));
        var u = System.getenv("GGTEST_MY_USER"); if (u != null && !u.isBlank()) { args.add("--user"); args.add(u); }
        var pw = System.getenv("GGTEST_MY_PASSWORD"); if (pw != null) { args.add("--password"); args.add(pw); }
        args.addAll(List.of(trailing));
        return runMain(args.toArray(String[]::new));
    }

    private Capture runMain(String... args) {
        var stdout = new ByteArrayOutputStream(); var stderr = new ByteArrayOutputStream();
        int code = Main.run(args, new PrintStream(stdout, true, StandardCharsets.UTF_8), new PrintStream(stderr, true, StandardCharsets.UTF_8), k -> null, tempDir);
        return new Capture(code, stdout.toString(StandardCharsets.UTF_8), stderr.toString(StandardCharsets.UTF_8));
    }

    private static Path fixture(String relative) {
        try { var u = MySqlCliIntegrationTest.class.getResource("/fixtures/my/" + relative);
            return Paths.get(Objects.requireNonNull(u).toURI()); } catch (URISyntaxException e) { throw new IllegalStateException(e); }
    }

    private record Capture(int exitCode, String stdout, String stderr) {
        int failures() { for (var line : stdout.split("\\R")) { var m = Pattern.compile("failed[=:\\s]+(\\d+)", Pattern.CASE_INSENSITIVE).matcher(line); if (m.find()) return Integer.parseInt(m.group(1)); } return 0; }
    }
}
