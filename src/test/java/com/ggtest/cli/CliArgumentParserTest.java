package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CliArgumentParserTest {

    @Test
    void parsesRequiredPositionalInputsWithOptionalUrlAndDefaultsAbsent() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "a.test",
            "dir/"
        });

        assertEquals(Optional.of("jdbc:sqlite::memory:"), parsed.url());
        assertTrue(parsed.user().isEmpty());
        assertTrue(parsed.password().isEmpty());
        assertTrue(parsed.engine().isEmpty());
        assertTrue(parsed.hashThreshold().isEmpty());
        assertTrue(parsed.envFile().isEmpty());
        assertTrue(parsed.color().isEmpty());
        assertEquals(List.of("a.test", "dir/"), parsed.inputs());
    }

    @Test
    void parsesColorFlag() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "--color", "never",
            "a.test"
        });
        assertEquals(Optional.of(ColorMode.NEVER), parsed.color());
    }

    @Test
    void haltFlagDefaultsToFalseWhenAbsent() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "a.test"
        });
        assertFalse(parsed.halt());
    }

    @Test
    void haltFlagIsSetWhenSupplied() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "--halt",
            "a.test"
        });
        assertTrue(parsed.halt());
    }

    @Test
    void repeatedHaltFlagIsEquivalentToSingle() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "--halt", "--halt", "--halt",
            "a.test"
        });
        assertTrue(parsed.halt());
    }

    @Test
    void singleDashHaltIsRejectedAsUnknownOption() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "-halt",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown")
                || ex.getMessage().contains("-halt"));
    }

    @Test
    void haltPrefixLongOptionIsRejectedAsUnknownOption() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--hal",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown")
                || ex.getMessage().contains("--hal"));
    }

    @Test
    void overrideFlagDefaultsToFalseWhenAbsent() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "a.test"
        });
        assertFalse(parsed.override());
    }

    @Test
    void overrideFlagIsSetWhenSupplied() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "--override",
            "a.test"
        });
        assertTrue(parsed.override());
    }

    @Test
    void repeatedOverrideFlagIsEquivalentToSingle() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "--override", "--override",
            "a.test"
        });
        assertTrue(parsed.override());
    }

    @Test
    void singleDashOverrideIsRejectedAsUnknownOption() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "-override",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown")
                || ex.getMessage().contains("-override"));
    }

    @Test
    void overridePrefixLongOptionIsRejectedAsUnknownOption() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--over",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("unknown")
                || ex.getMessage().contains("--over"));
    }

    @Test
    void parsesOptionalUserPasswordEngineHashThresholdAndEnvFile() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite:file.db",
            "--user", "u",
            "--password", "secret-password",
            "--engine", "sqlite",
            "--hash-threshold", "16",
            "--env-file", "/tmp/custom.env",
            "one.test"
        });

        assertEquals(Optional.of("jdbc:sqlite:file.db"), parsed.url());
        assertEquals(Optional.of("u"), parsed.user());
        assertEquals(Optional.of("secret-password"), parsed.password());
        assertEquals(Optional.of("sqlite"), parsed.engine());
        assertEquals(Optional.of(16), parsed.hashThreshold());
        assertEquals(Optional.of("/tmp/custom.env"), parsed.envFile());
        assertEquals(List.of("one.test"), parsed.inputs());
    }

    @Test
    void missingUrlIsAllowedAtParseTime() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {"a.test"});
        assertTrue(parsed.url().isEmpty());
        assertEquals(List.of("a.test"), parsed.inputs());
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
                    "--unknown-flag",
                    "a.test"
                }));
        assertTrue(!ex.getMessage().contains("super-secret-credential"));
    }

    @Test
    void parallelDefaultsToEmptyWhenAbsent() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "a.test"
        });
        assertTrue(parsed.parallel().isEmpty());
    }

    @Test
    void parsesParallelPositiveInteger() {
        ParsedArguments parsed = CliArgumentParser.parse(new String[] {
            "--url", "jdbc:sqlite::memory:",
            "--parallel", "2",
            "a.test"
        });
        assertEquals(Optional.of(2), parsed.parallel());
    }

    @Test
    void parallelZeroYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--parallel", "0",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("parallel"));
    }

    @Test
    void parallelNegativeYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--parallel", "-1",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("parallel"));
    }

    @Test
    void parallelNonIntegerYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--parallel", "abc",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("parallel"));
    }

    @Test
    void parallelMissingValueYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--parallel",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("parallel")
                || ex.getMessage().toLowerCase().contains("missing"));
    }

    @Test
    void parallelWithOverrideYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> CliArgumentParser.parse(new String[] {
                    "--url", "jdbc:sqlite::memory:",
                    "--parallel", "2",
                    "--override",
                    "a.test"
                }));
        assertTrue(ex.getMessage().toLowerCase().contains("parallel")
                && ex.getMessage().toLowerCase().contains("override"));
    }
}
