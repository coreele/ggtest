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
        assertTrue(joined.contains("at bad-parse.test"));
    }

    @Test
    void sqliteAssertionFailureIsNotHardError() throws Exception {
        Path file = fixture("fail.test");
        String display = "fail.test";

        FileOutcome outcome = runner.run(parser, file, display);

        assertEquals(FileBucket.FAILED, outcome.bucket());
        assertFalse(outcome.hardError());
        String joined = stripAnsi(String.join("\n", outcome.detailLines()));
        assertTrue(joined.contains("[WHY]"));
        assertFalse(joined.isBlank());
    }

    @Test
    void postgresTeardownFailureIsHardErrorWhenPgConfigured() throws Exception {
        String url = System.getenv("GGTEST_PG_URL");
        assumeTrue(url != null && !url.isBlank(), "GGTEST_PG_URL not set");

        errBuffer = new ByteArrayOutputStream();
        PrintStream err = new PrintStream(errBuffer);
        Optional<String> user = optionalEnv("GGTEST_PG_USER");
        Optional<String> password = optionalEnv("GGTEST_PG_PASSWORD");
        CliOptions pgOptions = new CliOptions(url, user, password, "postgres", 8, ColorMode.AUTO, List.of("x.test"));
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
        return new CliOptions(url, Optional.empty(), Optional.empty(), "sqlite", 8, ColorMode.AUTO, List.of("x.test"));
    }

    private static Optional<String> optionalEnv(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private static String stripAnsi(String text) {
        return ANSI.matcher(text).replaceAll("");
    }
}
