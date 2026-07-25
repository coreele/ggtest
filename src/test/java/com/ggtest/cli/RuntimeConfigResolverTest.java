package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeConfigResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void cliOverridesEnvAndDotEnv() throws IOException {
        writeEnv(
                """
                GGTEST_URL=jdbc:sqlite:from-file
                GGTEST_USER=file-user
                GGTEST_ENGINE=sqlite
                """);
        Map<String, String> process = Map.of(
                "GGTEST_URL", "jdbc:sqlite:from-env",
                "GGTEST_USER", "env-user");

        CliOptions options = resolve(
                parsed("--url", "jdbc:sqlite:from-cli", "--user", "cli-user", "a.test"),
                process::get);

        assertEquals("jdbc:sqlite:from-cli", options.url());
        assertEquals(Optional.of("cli-user"), options.user());
        assertEquals("sqlite", options.engine());
    }

    @Test
    void envOverridesDotEnvWhenCliAbsent() throws IOException {
        writeEnv("GGTEST_URL=jdbc:sqlite:from-file\nGGTEST_PASSWORD=file-secret\n");
        Map<String, String> process = Map.of("GGTEST_URL", "jdbc:sqlite:from-env");

        CliOptions options = resolve(parsed("a.test"), process::get);

        assertEquals("jdbc:sqlite:from-env", options.url());
        assertEquals(Optional.of("file-secret"), options.password());
    }

    @Test
    void onlyDotEnvUrlIsAccepted() throws IOException {
        writeEnv("GGTEST_URL=jdbc:sqlite::memory:\n");

        CliOptions options = resolve(parsed("a.test"), key -> null);

        assertEquals("jdbc:sqlite::memory:", options.url());
        assertEquals("sqlite", options.engine());
    }

    @Test
    void missingUrlFromAllSourcesYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class, () -> resolve(parsed("a.test"), key -> null));
        assertTrue(ex.getMessage().toLowerCase().contains("url"));
    }

    @Test
    void defaultCwdDotEnvMissingIsOkWithCliUrl() {
        CliOptions options = resolve(
                parsed("--url", "jdbc:sqlite::memory:", "a.test"), key -> null);
        assertEquals("jdbc:sqlite::memory:", options.url());
    }

    @Test
    void explicitEnvFileReplacesDefaultCwdDotEnv() throws IOException {
        writeEnv("GGTEST_URL=jdbc:sqlite:cwd-file\n");
        Path other = tempDir.resolve("other.env");
        Files.writeString(other, "GGTEST_URL=jdbc:sqlite:other-file\n");

        CliOptions options = resolve(
                parsed("--env-file", other.toString(), "a.test"), key -> null);

        assertEquals("jdbc:sqlite:other-file", options.url());
    }

    @Test
    void explicitMissingEnvFileYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> resolve(
                        parsed("--env-file", tempDir.resolve("nope.env").toString(), "a.test"),
                        key -> null));
        assertTrue(ex.getMessage().toLowerCase().contains("env"));
    }

    @Test
    void allowsPostgresEngineWithMatchingUrl() {
        CliOptions options = resolve(
                parsed(
                        "--url", "jdbc:postgresql://localhost/db",
                        "--engine", "POSTGRES",
                        "a.test"),
                key -> null);
        assertEquals("postgres", options.engine());
    }

    @Test
    void unknownEngineYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> resolve(
                        parsed("--url", "jdbc:sqlite::memory:", "--engine", "mysql", "a.test"),
                        key -> null));
        assertTrue(ex.getMessage().toLowerCase().contains("engine"));
    }

    @Test
    void engineUrlMismatchYieldsUsageError() {
        UsageException sqliteMismatch = assertThrows(
                UsageException.class,
                () -> resolve(
                        parsed(
                                "--url", "jdbc:postgresql://localhost/db",
                                "--engine", "sqlite",
                                "a.test"),
                        key -> null));
        assertTrue(sqliteMismatch.getMessage().toLowerCase().contains("sqlite")
                || sqliteMismatch.getMessage().toLowerCase().contains("url"));

        UsageException pgMismatch = assertThrows(
                UsageException.class,
                () -> resolve(
                        parsed(
                                "--url", "jdbc:sqlite::memory:",
                                "--engine", "postgres",
                                "a.test"),
                        key -> null));
        assertTrue(pgMismatch.getMessage().toLowerCase().contains("postgres")
                || pgMismatch.getMessage().toLowerCase().contains("url"));
    }

    @Test
    void engineUrlMismatchFromDotEnvYieldsUsageError() throws IOException {
        writeEnv(
                """
                GGTEST_URL=jdbc:sqlite::memory:
                GGTEST_ENGINE=postgres
                """);
        UsageException ex = assertThrows(
                UsageException.class, () -> resolve(parsed("a.test"), key -> null));
        assertTrue(ex.getMessage().toLowerCase().contains("postgres")
                || ex.getMessage().toLowerCase().contains("url"));
    }

    @Test
    void doesNotReadGgtestPgGateKeysAsRuntimeConfig() throws IOException {
        writeEnv("GGTEST_URL=jdbc:sqlite::memory:\n");
        Map<String, String> process = new HashMap<>();
        process.put("GGTEST_PG_URL", "jdbc:postgresql://should-not-be-used/db");
        process.put("GGTEST_PG_PASSWORD", "gate-secret");

        CliOptions options = resolve(parsed("a.test"), process::get);

        assertEquals("jdbc:sqlite::memory:", options.url());
        assertTrue(options.password().isEmpty());
    }

    @Test
    void cliOptionsToStringRedactsPassword() {
        CliOptions options = new CliOptions(
                "jdbc:sqlite::memory:",
                Optional.of("u"),
                Optional.of("super-secret-credential"),
                "sqlite",
                8,
                java.util.List.of("a.test"));
        assertFalse(options.toString().contains("super-secret-credential"));
        assertTrue(options.toString().contains("***"));
    }

    private void writeEnv(String content) throws IOException {
        Files.writeString(tempDir.resolve(".env"), content);
    }

    private CliOptions resolve(ParsedArguments parsed, Function<String, String> env) {
        return RuntimeConfigResolver.resolve(parsed, env, tempDir);
    }

    private static ParsedArguments parsed(String... args) {
        return CliArgumentParser.parse(args);
    }
}
