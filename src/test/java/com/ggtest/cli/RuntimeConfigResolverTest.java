package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.normalize.ResultComparer;
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
    void nonEmptyPasswordFromCliIsAssembledForPostgres() {
        CliOptions options = resolve(
                parsed(
                        "--url",
                        "jdbc:postgresql://localhost/db",
                        "--engine",
                        "postgres",
                        "--password",
                        "nonempty-assembly-secret",
                        "a.test"),
                key -> null);

        assertEquals(Optional.of("nonempty-assembly-secret"), options.password());
        assertEquals("postgres", options.engine());
    }

    @Test
    void nonEmptyPasswordFromProcessEnvIsAssembledForPostgres() {
        Map<String, String> process = Map.of(
                "GGTEST_URL", "jdbc:postgresql://localhost/db",
                "GGTEST_ENGINE", "postgres",
                "GGTEST_PASSWORD", "nonempty-env-assembly-secret");

        CliOptions options = resolve(parsed("a.test"), process::get);

        assertEquals(Optional.of("nonempty-env-assembly-secret"), options.password());
        assertEquals("postgres", options.engine());
    }

    @Test
    void cliOptionsToStringRedactsPassword() {
        CliOptions options = new CliOptions(
                "jdbc:sqlite::memory:",
                Optional.of("u"),
                Optional.of("super-secret-credential"),
                "sqlite",
                8,
                ColorMode.AUTO,
                java.util.List.of("a.test"));
        assertFalse(options.toString().contains("super-secret-credential"));
        assertTrue(options.toString().contains("***"));
    }

    @Test
    void defaultHashThresholdIsEight() {
        CliOptions options = resolve(
                parsed("--url", "jdbc:sqlite::memory:", "a.test"), key -> null);
        assertEquals(ResultComparer.DEFAULT_HASH_THRESHOLD, options.hashThreshold());
    }

    @Test
    void cliOptionsToStringRedactsUrlUserInfo() {
        CliOptions options = new CliOptions(
                "jdbc:postgresql://alice:bob@localhost/mydb",
                Optional.of("alice"),
                Optional.of("plain-password-value"),
                "postgres",
                8,
                ColorMode.AUTO,
                java.util.List.of("a.test"));
        String dump = options.toString();

        assertFalse(dump.contains("bob"), "URL userinfo password must not appear");
        assertFalse(dump.contains("plain-password-value"), "password field must not appear");
        assertTrue(dump.contains("***"), "password field must be masked");
        assertTrue(dump.contains("localhost"), "host remains readable");
    }

    @Test
    void colorDefaultsToAuto() {
        CliOptions options = resolve(
                parsed("--url", "jdbc:sqlite::memory:", "a.test"), key -> null);
        assertEquals(ColorMode.AUTO, options.colorMode());
    }

    @Test
    void colorCliBeatsPropertyAndEnv() {
        Map<String, String> env = Map.of(RuntimeConfigResolver.COLOR_ENV, "never");
        CliOptions options = RuntimeConfigResolver.resolve(
                parsed("--url", "jdbc:sqlite::memory:", "--color", "always", "a.test"),
                env::get,
                tempDir,
                key -> RuntimeConfigResolver.COLOR_PROPERTY.equals(key) ? "never" : null);
        assertEquals(ColorMode.ALWAYS, options.colorMode());
    }

    @Test
    void colorPropertyBeatsEnvWhenCliAbsent() {
        Map<String, String> env = Map.of(RuntimeConfigResolver.COLOR_ENV, "always");
        CliOptions options = RuntimeConfigResolver.resolve(
                parsed("--url", "jdbc:sqlite::memory:", "a.test"),
                env::get,
                tempDir,
                key -> RuntimeConfigResolver.COLOR_PROPERTY.equals(key) ? "never" : null);
        assertEquals(ColorMode.NEVER, options.colorMode());
    }

    @Test
    void invalidColorYieldsUsageError() {
        UsageException ex = assertThrows(
                UsageException.class,
                () -> resolve(
                        parsed("--url", "jdbc:sqlite::memory:", "--color", "rainbow", "a.test"),
                        key -> null));
        assertTrue(ex.getMessage().toLowerCase().contains("color"));
    }

    @Test
    void resolveAnsiEnabledAutoFollowsInjectedTty() {
        assertTrue(RuntimeConfigResolver.resolveAnsiEnabled(ColorMode.AUTO, true));
        assertFalse(RuntimeConfigResolver.resolveAnsiEnabled(ColorMode.AUTO, false));
    }

    @Test
    void resolveAnsiEnabledAlwaysAndNeverIgnoreTty() {
        assertTrue(RuntimeConfigResolver.resolveAnsiEnabled(ColorMode.ALWAYS, false));
        assertTrue(RuntimeConfigResolver.resolveAnsiEnabled(ColorMode.ALWAYS, true));
        assertFalse(RuntimeConfigResolver.resolveAnsiEnabled(ColorMode.NEVER, true));
        assertFalse(RuntimeConfigResolver.resolveAnsiEnabled(ColorMode.NEVER, false));
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
