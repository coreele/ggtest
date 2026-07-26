package com.ggtest.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * P0 credential redaction: proves sanitize strips URL userinfo and configured password literals.
 * Must fail on strip-only {@link CliSession#sanitize} (P0-4 red phase).
 */
class CredentialRedactionTest {

    private static CliSession session(CliOptions options) {
        return new CliSession(options, new PrintStream(System.out), new PrintStream(System.err), false);
    }

    private static CliOptions options(String url, Optional<String> password) {
        return new CliOptions(
                url,
                Optional.empty(),
                password,
                "sqlite",
                8,
                ColorMode.AUTO,
                List.of("a.test"));
    }

    @Test
    void sanitizeRedactsUrlUserInfo() {
        CliSession session = session(options("jdbc:postgresql://alice:secretPass@host/db", Optional.empty()));
        String message = "connection failed: jdbc:postgresql://alice:secretPass@host/db";
        String sanitized = session.sanitize(message);

        assertFalse(sanitized.contains("secretPass"), "userinfo password must not appear");
        assertTrue(sanitized.contains("host"), "host fragment remains identifiable");
        assertTrue(sanitized.contains("connection failed"), "error prefix remains identifiable");
    }

    @Test
    void sanitizeRedactsConfiguredPasswordLiteral() {
        CliSession session = session(options("jdbc:sqlite::memory:", Optional.of("super-secret-credential")));
        String message = "authentication failed: super-secret-credential rejected";
        String sanitized = session.sanitize(message);

        assertFalse(sanitized.contains("super-secret-credential"));
        assertTrue(sanitized.contains("authentication failed"));
    }

    @Test
    void sanitizePreservesPlainMessagesWithoutCredentials() {
        CliSession session = session(options("jdbc:sqlite::memory:", Optional.empty()));
        String message = "parse error: unexpected token at line 3";
        assertEquals(message, session.sanitize(message));
    }

    @Test
    void sanitizeNullBecomesEmpty() {
        CliSession session = session(options("jdbc:sqlite::memory:", Optional.empty()));
        assertEquals("", session.sanitize(null));
    }

    @Test
    void sanitizeStripsLeadingAndTrailingWhitespace() {
        CliSession session = session(options("jdbc:sqlite::memory:", Optional.empty()));
        assertEquals("plain error", session.sanitize("  plain error  "));
    }
}
