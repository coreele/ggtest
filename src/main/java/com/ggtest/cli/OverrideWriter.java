package com.ggtest.cli;

import com.ggtest.model.QueryRecord;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.model.StatementRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

/**
 * Rewrites the expected intervals of overridden records in a source file and
 * writes the result atomically.
 *
 * <p>This is the only class that touches the disk for write-back. It consumes
 * the expected-interval fields populated by the parser ({@link QueryRecord#expectedHeaderLine()},
 * {@link QueryRecord#expectedBodyEndLine()}, {@link StatementRecord#errorMsgStartColumn()})
 * and performs byte-level interval replacement, preserving every other byte
 * (comments, whitespace, SQL, other records, headers, EOL style, trailing newline).
 */
@FunctionalInterface
interface FileMover {
    void move(Path source, Path target, CopyOption... options) throws IOException;
}

final class OverrideWriter {

    private final FileMover mover;

    OverrideWriter() {
        this((source, target, options) -> Files.move(source, target, options));
    }

    OverrideWriter(FileMover mover) {
        this.mover = Objects.requireNonNull(mover, "mover");
    }

    /**
     * A single override to apply to a source file.
     *
     * @param record           the record being overridden
     * @param newText          golden text: query expected body, or statement error message
     * @param newSignature     when non-null, rewrite the query header type signature to this
     *                         (I/R/T string)
     * @param separator        when non-null, declare {@code separator=<separator>} on the query header
     * @param toStatementError when {@code true}, rewrite the record as {@code statement error <newText>}
     */
    record Override(SqlTestRecord record, String newText, String newSignature, String separator, boolean toStatementError) {

        public Override {
            Objects.requireNonNull(record, "record");
            Objects.requireNonNull(newText, "newText");
        }

        Override(SqlTestRecord record, String newText) {
            this(record, newText, null, null, false);
        }

        static Override expected(SqlTestRecord record, String newText) {
            return new Override(record, newText);
        }

        static Override querySignature(SqlTestRecord record, String signature, String separator, String newText) {
            return new Override(record, newText, signature, separator, false);
        }

        static Override statementError(SqlTestRecord record, String message) {
            return new Override(record, message, null, null, true);
        }
    }

    /**
     * Rewrites {@code content} by applying all {@code overrides} to their respective expected
     * intervals. EOL style is detected from the content and preserved; the trailing newline
     * (if any) is preserved. Overrides are applied in descending start-line order so that
     * line-range edits to later records do not shift the indices of earlier records.
     *
     * @param content   the original file text (UTF-8)
     * @param overrides the overrides to apply
     * @return the rewritten text
     */
    String rewrite(String content, List<Override> overrides) {
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(overrides, "overrides");
        if (overrides.isEmpty()) {
            return content;
        }

        String eol = content.contains("\r\n") ? "\r\n" : "\n";
        List<String> lines = splitOnEol(content, eol);

        List<Override> sorted = new ArrayList<>(overrides);
        sorted.sort(Comparator.comparingInt((Override o) -> o.record().location().startLine()).reversed());
        for (Override ov : sorted) {
            applyOverride(lines, ov);
        }

        return String.join(eol, lines);
    }

    /**
     * Writes {@code newText} to {@code target} atomically: a temp file is created in the
     * target's directory, filled, then renamed via {@link StandardCopyOption#ATOMIC_MOVE}.
     * If atomic move is unsupported, {@link StandardCopyOption#REPLACE_EXISTING} is used.
     * On any failure the temp file is deleted and an {@link IOException} is thrown; the
     * original file is never opened for writing and thus never damaged.
     *
     * @param target  the file to overwrite
     * @param newText the new file content (UTF-8)
     * @throws IOException if the temp file cannot be created, written, or moved
     */
    void writeAtomically(Path target, String newText) throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(newText, "newText");
        Path dir = target.toAbsolutePath().getParent();
        Path temp = Files.createTempFile(dir, ".ggtest-override-", ".tmp");
        try {
            Files.writeString(temp, newText, StandardCharsets.UTF_8);
            try {
                mover.move(temp, target.toAbsolutePath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                mover.move(temp, target.toAbsolutePath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            if (ex instanceof IOException io) {
                throw io;
            }
            throw new IOException(ex);
        }
    }

    private void applyOverride(List<String> lines, Override ov) {
        SqlTestRecord record = ov.record();
        if (ov.toStatementError()) {
            if (record instanceof QueryRecord query) {
                convertQueryToStatementError(lines, query, ov.newText());
            } else if (record instanceof StatementRecord stmt) {
                convertStatementToError(lines, stmt, ov.newText());
            }
            return;
        }
        if (record instanceof QueryRecord query) {
            applyQueryOverride(lines, query, ov.newText(), ov.newSignature(), ov.separator());
        } else if (record instanceof StatementRecord stmt) {
            applyStatementOverride(lines, stmt, ov.newText());
        }
    }

    private void applyQueryOverride(
            List<String> lines, QueryRecord query, String newText, String newSignature, String separator) {
        if (newSignature != null || separator != null) {
            int headerIdx = query.location().startLine() - 1;
            if (headerIdx >= 0 && headerIdx < lines.size()) {
                lines.set(headerIdx, rewriteQueryHeader(lines.get(headerIdx), newSignature, separator));
            }
        }
        int headerLine = query.expectedHeaderLine();
        int bodyEnd = query.expectedBodyEndLine();
        if (headerLine <= 0) {
            return;
        }
        int from = headerLine;
        int to = bodyEnd;
        if (from < to) {
            lines.subList(from, to).clear();
        }
        List<String> newBody = splitOnEol(newText, "\n");
        lines.addAll(from, newBody);
    }

    /** Rewrites the query header's type signature and/or separator attribute in place. */
    private static String rewriteQueryHeader(String line, String newSignature, String separator) {
        String result = line;
        if (newSignature != null) {
            result = result.replaceFirst("^(\\s*query\\s+)\\S+", "$1" + newSignature);
        }
        if (separator != null) {
            if (result.matches(".*\\sseparator=\\S+.*")) {
                result = result.replaceFirst("separator=\\S+", Matcher.quoteReplacement("separator=" + separator));
            } else {
                result = result + " separator=" + separator;
            }
        }
        return result;
    }

    /** Rewrites a failed query into {@code statement error <message>}, dropping its expectation block. */
    private void convertQueryToStatementError(List<String> lines, QueryRecord query, String message) {
        int headerIdx = query.location().startLine() - 1;
        if (headerIdx < 0 || headerIdx >= lines.size()) {
            return;
        }
        lines.set(headerIdx, "statement error " + message);
        int headerLine = query.expectedHeaderLine();
        if (headerLine > 0) {
            int from = headerLine - 1;
            int to = query.expectedBodyEndLine();
            if (from >= 0 && from <= to && to <= lines.size()) {
                lines.subList(from, to).clear();
            }
        }
    }

    /** Rewrites a failed {@code statement ok} into {@code statement error <message>}. */
    private void convertStatementToError(List<String> lines, StatementRecord stmt, String message) {
        int idx = stmt.location().startLine() - 1;
        if (idx < 0 || idx >= lines.size()) {
            return;
        }
        String line = lines.get(idx);
        lines.set(idx, line.replace("statement ok", "statement error " + message));
    }

    private void applyStatementOverride(List<String> lines, StatementRecord stmt, String newText) {
        int col = stmt.errorMsgStartColumn();
        if (col < 0) {
            return;
        }
        int lineIdx = stmt.location().startLine() - 1;
        if (lineIdx < 0 || lineIdx >= lines.size()) {
            return;
        }
        String line = lines.get(lineIdx);
        int end = Math.min(col, line.length());
        String text = newText;
        if (stmt.timeoutMs() > 0) {
            text = text + " timeout=" + stmt.timeoutMs();
        }
        if (stmt.conn() != null) {
            text = text + " conn=" + stmt.conn();
        }
        lines.set(lineIdx, line.substring(0, end) + text);
    }

    private static List<String> splitOnEol(String text, String eol) {
        List<String> result = new ArrayList<>();
        int start = 0;
        int idx;
        while ((idx = text.indexOf(eol, start)) >= 0) {
            result.add(text.substring(start, idx));
            start = idx + eol.length();
        }
        result.add(text.substring(start));
        return result;
    }
}
