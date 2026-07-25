package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CliArgumentParserTest {

    @Test
    void parsesRequiredUrlAndPositionalInputsWithDefaults() {
        CliOptions options = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "a.test",
            "dir/"
        });

        assertEquals("jdbc:sqlite::memory:", options.url());
        assertTrue(options.user().isEmpty());
        assertTrue(options.password().isEmpty());
        assertEquals("sqlite", options.engine());
        assertEquals(8, options.hashThreshold());
        assertEquals(List.of("a.test", "dir/"), options.inputs());
    }

    @Test
    void parsesOptionalUserPasswordEngineAndHashThreshold() {
        CliOptions options = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite:file.db",
            "--user", "u",
            "--password", "secret-password",
            "--engine", "sqlite",
            "--hash-threshold", "16",
            "one.test"
        });

        assertEquals("jdbc:sqlite:file.db", options.url());
        assertEquals("u", options.user().orElseThrow());
        assertEquals("secret-password", options.password().orElseThrow());
        assertEquals("sqlite", options.engine());
        assertEquals(16, options.hashThreshold());
        assertEquals(List.of("one.test"), options.inputs());
    }

    @Test
    void missingUrlYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {"a.test"}));
        assertTrue(ex.getMessage().toLowerCase().contains("url"));
    }

    @Test
    void missingPositionalInputsYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {"--url", "jdbc:sqlite::memory:"}));
        assertTrue(ex.getMessage().toLowerCase().contains("file")
                || ex.getMessage().toLowerCase().contains("input")
                || ex.getMessage().toLowerCase().contains("path"));
    }

    @Test
    void unsupportedEngineYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--engine", "postgres",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("engine"));
    }

    @Test
    void unknownOptionYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--unknown",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown")
                || ex.getMessage().toLowerCase().contains("--unknown"));
    }

    @Test
    void nonIntegerHashThresholdYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--hash-threshold", "x",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("hash-threshold"));
    }

    @Test
    void usageErrorMessageDoesNotContainPasswordValue() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--password", "super-secret-credential",
                    "--engine", "postgres",
                    "a.test"
                }));
        assertTrue(!ex.getMessage().contains("super-secret-credential"));
    }
}
