package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * P0 credential redaction: proves sanitize strips URL userinfo and configured password literals.
 */
class CredentialRedactionTest {

    private static FileRunner fileRunner(CliOptions options) {
        ReportWriter reportWriter =
                new ReportWriter(new PrintStream(new ByteArrayOutputStream()), new ReportStyle(false));
        return new FileRunner(options, new PrintStream(System.err), reportWriter);
    }

    private static CliOptions options(String url, Optional<String> password) {
        return new CliOptions(
                url,
                Optional.empty(),
                password,
                "sqlite",
                8,
                ColorMode.AUTO,
                false,
                List.of("a.test"));
    }

    @Test
    void sanitizeRedactsUrlUserInfo() {
        FileRunner runner = fileRunner(options("jdbc:postgresql://alice:secretPass@host/db", Optional.empty()));
        String message = "connection failed: jdbc:postgresql://alice:secretPass@host/db";
        String sanitized = runner.sanitize(message);

        assertFalse(sanitized.contains("secretPass"), "userinfo password must not appear");
        assertTrue(sanitized.contains("host"), "host fragment remains identifiable");
        assertTrue(sanitized.contains("connection failed"), "error prefix remains identifiable");
    }

    @Test
    void sanitizeRedactsConfiguredPasswordLiteral() {
        FileRunner runner = fileRunner(options("jdbc:sqlite::memory:", Optional.of("super-secret-credential")));
        String message = "authentication failed: super-secret-credential rejected";
        String sanitized = runner.sanitize(message);

        assertFalse(sanitized.contains("super-secret-credential"));
        assertTrue(sanitized.contains("authentication failed"));
    }

    @Test
    void sanitizePreservesPlainMessagesWithoutCredentials() {
        FileRunner runner = fileRunner(options("jdbc:sqlite::memory:", Optional.empty()));
        String message = "parse error: unexpected token at line 3";
        assertEquals(message, runner.sanitize(message));
    }

    @Test
    void sanitizeNullBecomesEmpty() {
        FileRunner runner = fileRunner(options("jdbc:sqlite::memory:", Optional.empty()));
        assertEquals("", runner.sanitize(null));
    }

    @Test
    void sanitizeStripsLeadingAndTrailingWhitespace() {
        FileRunner runner = fileRunner(options("jdbc:sqlite::memory:", Optional.empty()));
        assertEquals("plain error", runner.sanitize("  plain error  "));
    }
}
