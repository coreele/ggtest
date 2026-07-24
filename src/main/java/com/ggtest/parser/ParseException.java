package com.ggtest.parser;

/**
 * Thrown when the parser encounters malformed sqllogictest input (unknown record
 * type, illegal type signature, truncated record, etc.).
 *
 * <p>Carries the source name, the 1-based line number and a human-readable reason
 * so callers (e.g. the {@code cli-corpus} slice) can produce a locatable error
 * report and map it to a parse-error exit code. It is separate from
 * {@link java.io.IOException} (which signals an unreadable file) so callers can
 * distinguish "file cannot be read" from "content is invalid".
 *
 * <p>Unchecked by design: parsing fails fast on the first invalid input and the
 * exception propagates to a top-level handler without threading {@code throws}
 * through the call chain.
 */
public class ParseException extends RuntimeException {

    private final String sourceName;
    private final int lineNumber;
    private final String reason;

    /**
     * @param sourceName logical name of the source (usually the file path)
     * @param lineNumber 1-based line number where the error was detected
     * @param reason     human-readable description of the problem
     */
    public ParseException(String sourceName, int lineNumber, String reason) {
        super(sourceName + ":" + lineNumber + ": " + reason);
        this.sourceName = sourceName;
        this.lineNumber = lineNumber;
        this.reason = reason;
    }

    /** Logical name of the source (usually the file path). */
    public String sourceName() {
        return sourceName;
    }

    /** 1-based line number where the error was detected. */
    public int lineNumber() {
        return lineNumber;
    }

    /** Human-readable description of the problem. */
    public String reason() {
        return reason;
    }
}
