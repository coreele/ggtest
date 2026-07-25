package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DotEnvLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsWhitelistKeysIgnoresUnknownAndComments() throws IOException {
        Path env = tempDir.resolve("sample.env");
        Files.writeString(
                env,
                """
                # comment
                GGTEST_URL=jdbc:sqlite::memory:
                GGTEST_USER=alice
                GGTEST_PASSWORD="secret"
                GGTEST_ENGINE='sqlite'
                GGTEST_HASH_THRESHOLD=16
                UNKNOWN_KEY=nope
                malformed-line
                """);

        Map<String, String> values = DotEnvLoader.load(env);

        assertEquals("jdbc:sqlite::memory:", values.get("GGTEST_URL"));
        assertEquals("alice", values.get("GGTEST_USER"));
        assertEquals("secret", values.get("GGTEST_PASSWORD"));
        assertEquals("sqlite", values.get("GGTEST_ENGINE"));
        assertEquals("16", values.get("GGTEST_HASH_THRESHOLD"));
        assertFalse(values.containsKey("UNKNOWN_KEY"));
    }

    @Test
    void unreadablePathYieldsUsageError() {
        Path missing = tempDir.resolve("missing.env");
        UsageException ex = assertThrows(UsageException.class, () -> DotEnvLoader.load(missing));
        assertTrue(ex.getMessage().toLowerCase().contains("env"));
    }

    @Test
    void parseLineStripsMatchingQuotes() {
        assertEquals(
                Optional.of(new DotEnvLoader.Entry("GGTEST_USER", "bob")),
                DotEnvLoader.parseLine("GGTEST_USER='bob'"));
        assertEquals(
                Optional.of(new DotEnvLoader.Entry("GGTEST_USER", "bob")),
                DotEnvLoader.parseLine("GGTEST_USER=\"bob\""));
    }
}
