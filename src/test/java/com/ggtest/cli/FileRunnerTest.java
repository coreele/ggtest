package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ggtest.parser.SqlLogicTestParser;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link FileRunner} per-file execution mapping. */
class FileRunnerTest {

    private static final Pattern ANSI = Pattern.compile("\\u001B\\[[0-9;]*m");

    private FileRunner runner;
    private ByteArrayOutputStream errBuffer;
    private SqlLogicTestParser parser;

    @BeforeEach
    void setUp() {
        errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer);
        CliOptions options = sqliteOptions("jdbc:sqlite::memory:");
        ReportWriter reportWriter = new ReportWriter(new PrintStream(new ByteArrayOutputStream()), new ReportStyle(false));
        runner = new FileRunner(options, err, reportWriter);
        parser = new SqlLogicTestParser();
    }

    @Test
    void parseErrorIsHardFailureWithDetail() throws Exception {
        Path file = fixture("bad-parse.test");
        String display = "bad-parse.test";

        FileOutcome outcome = runner.run(parser, file, display);

        assertEquals(FileBucket.FAILED, outcome.bucket());
        assertTrue(outcome.hardError());
        String joined = stripAnsi(String.join("\n", outcome.detailLines()));
        assertTrue(joined.contains("parse error:"));
        assertTrue(joined.contains("    at bad-parse.test"), "first line: " + joined);
    }

    @Test
    void sqliteAssertionFailureIsNotHardError() throws Exception {
        Path file = fixture("fail.test");
        String display = "fail.test";

        FileOutcome outcome = runner.run(parser, file, display);

        assertEquals(FileBucket.FAILED, outcome.bucket());
        assertFalse(outcome.hardError());
        String joined = stripAnsi(String.join("\n", outcome.detailLines()));
        assertFalse(joined.isBlank());
        assertAtIndent4(outcome.detailLines());
    }

    @Test
    void multiFailureHasNoBlankSeparator() throws Exception {
        Path file = fixture("multi-fail.test");
        FileOutcome outcome = runner.run(parser, file, "multi-fail.test");

        assertEquals(FileBucket.FAILED, outcome.bucket());
        assertFalse(outcome.hardError());
        List<String> lines = outcome.detailLines();
        assertAtIndent4(lines);

        long atCount = lines.stream().filter(l -> stripAnsi(l).trim().startsWith("at ")).count();
        assertEquals(3, atCount, () -> String.join("\n", lines));

        List<Integer> atIndexes = new java.util.ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            if (stripAnsi(lines.get(i)).trim().startsWith("at ")) {
                atIndexes.add(i);
            }
        }
        assertEquals(3, atIndexes.size(), () -> String.join("\n", lines));

        String joined = stripAnsi(String.join("\n", lines));
        assertFalse(joined.contains("[1/"), joined);
        assertFalse(joined.contains("failures in file"), joined);
        assertFalse(joined.contains("reason="), joined);
    }

    @Test
    void haltStopsAfterFirstFailureInOneFile() throws Exception {
        errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer);
        CliOptions haltOptions = sqliteOptionsWithHalt("jdbc:sqlite::memory:");
        ReportWriter reportWriter = new ReportWriter(new PrintStream(new ByteArrayOutputStream()), new ReportStyle(false));
        FileRunner haltRunner = new FileRunner(haltOptions, err, reportWriter);

        Path file = fixture("multi-fail.test");
        FileOutcome outcome = haltRunner.run(parser, file, "multi-fail.test");

        assertEquals(FileBucket.FAILED, outcome.bucket());
        assertFalse(outcome.hardError(), "--halt stops on first failure but is not itself a hard error");
        List<String> lines = outcome.detailLines();
        assertAtIndent4(lines);

        long atCount = lines.stream().filter(l -> stripAnsi(l).trim().startsWith("at ")).count();
        assertEquals(1, atCount, "exactly one failure detail block under --halt");
    }

    @Test
    void hardErrorDetailHasAtWithFourSpaceIndent() throws Exception {
        Path file = fixture("bad-parse.test");
        FileOutcome outcome = runner.run(parser, file, "bad-parse.test");

        assertTrue(outcome.hardError());
        assertAtIndent4(outcome.detailLines());
    }

    private static void assertAtIndent4(List<String> lines) {
        boolean sawAt = false;
        for (String raw : lines) {
            String line = stripAnsi(raw);
            if (line.trim().startsWith("at ")) {
                assertTrue(line.startsWith("    "), "at line must have four-space indent: [" + raw + "]");
                sawAt = true;
            }
        }
        assertTrue(sawAt, "expected at least one at line in: " + String.join("\n", lines));
    }

    @Test
    void postgresTeardownFailureIsHardErrorWhenPgConfigured() throws Exception {
        String url = System.getenv("GGTEST_PG_URL");
        assumeTrue(url != null && !url.isBlank(), "GGTEST_PG_URL not set");

        errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer);
        Optional<String> user = optionalEnv("GGTEST_PG_USER");
        Optional<String> password = optionalEnv("GGTEST_PG_PASSWORD");
        CliOptions pgOptions = new CliOptions(url, user, password, "postgres", 8, ColorMode.AUTO, false, List.of("x.test"));
        ReportWriter reportWriter =
                new ReportWriter(new PrintStream(new ByteArrayOutputStream()), new ReportStyle(false));
        FileRunner pgRunner = new FileRunner(pgOptions, err, reportWriter);

        Path file = pgFixture("basic.test");
        FileOutcome outcome = pgRunner.run(parser, file, "basic.test");

        assertFalse(outcome.hardError(), () -> "basic fixture should pass: " + stripAnsi(String.join("\n", outcome.detailLines())));
        assertEquals(FileBucket.PASSED, outcome.bucket());
    }

    private static Path fixture(String name) throws URISyntaxException {
        URL url = FileRunnerTest.class.getResource("/fixtures/cli/" + name);
        if (url == null) {
            throw new IllegalStateException("missing fixture: " + name);
        }
        return Paths.get(url.toURI());
    }

    private static Path pgFixture(String name) throws URISyntaxException {
        URL url = FileRunnerTest.class.getResource("/fixtures/pg/" + name);
        if (url == null) {
            throw new IllegalStateException("missing pg fixture: " + name);
        }
        return Paths.get(url.toURI());
    }

    private static CliOptions sqliteOptions(String url) {
        return new CliOptions(url, Optional.empty(), Optional.empty(), "sqlite", 8, ColorMode.AUTO, false, List.of("x.test"));
    }

    private static CliOptions sqliteOptionsWithHalt(String url) {
        return new CliOptions(url, Optional.empty(), Optional.empty(), "sqlite", 8, ColorMode.AUTO, true, List.of("x.test"));
    }

    private static Optional<String> optionalEnv(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static String stripAnsi(String text) {
        return ANSI.matcher(text).replaceAll("");
    }
}
