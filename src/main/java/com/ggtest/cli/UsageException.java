package com.ggtest.cli;

/**
 * Thrown when CLI arguments are missing, unknown, or otherwise invalid.
 * Callers map this to exit code 2 without connecting to a database.
 *
 * <p>Unchecked so CLI entry points can handle it at the top level without
 * threading {@code throws} through orchestration helpers.
 */
public final class UsageException extends RuntimeException {

    public UsageException(String message) {
        super(message);
    }
}
