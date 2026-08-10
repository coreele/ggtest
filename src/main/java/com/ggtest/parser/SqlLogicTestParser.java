package com.ggtest.parser;

import com.ggtest.model.ColumnType;
import com.ggtest.model.HaltRecord;
import com.ggtest.model.HashThresholdRecord;
import com.ggtest.model.OnlyIfRecord;
import com.ggtest.model.QueryRecord;
import com.ggtest.model.SkipIfRecord;
import com.ggtest.model.SortMode;
import com.ggtest.model.SourceLocation;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.model.StatementExpectation;
import com.ggtest.model.StatementRecord;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Parses sqllogictest-format UTF-8 text into an ordered sequence of
 * {@link SqlTestRecord} values.
 *
 * <p>Does not inspect file extensions: the same content yields semantically
 * equivalent records whether read from a {@code .test}, {@code .slt}, or
 * extensionless path. Does not connect to a database or evaluate directives.
 */
public final class SqlLogicTestParser {

    /**
     * Reads {@code file} as UTF-8 and parses it. The source name used in
     * locations and errors is {@code file.toString()}.
     *
     * @param file path to a sqllogictest input file
     * @return ordered records
     * @throws IOException     if the file cannot be read
     * @throws ParseException  if the content is malformed
     */
    public List<SqlTestRecord> parse(Path file) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        return parse(file.toString(), content);
    }

    /**
     * Parses sqllogictest content from a string.
     *
     * @param sourceName logical source name for locations and error messages
     * @param content    UTF-8 sqllogictest text
     * @return ordered records
     * @throws ParseException if the content is malformed
     */
    public List<SqlTestRecord> parse(String sourceName, String content) {
        LineBuffer lines = LineBuffer.from(content);
        List<SqlTestRecord> records = new ArrayList<>();
        while (true) {
            skipTrivia(lines);
            if (!lines.hasNext()) {
                break;
            }
            records.add(parseRecord(sourceName, lines));
        }
        return List.copyOf(records);
    }

    private static void skipTrivia(LineBuffer lines) {
        while (lines.hasNext()) {
            String line = lines.peek();
            if (line.isEmpty() || line.startsWith("#")) {
                lines.next();
                continue;
            }
            break;
        }
    }

    private static SqlTestRecord parseRecord(String sourceName, LineBuffer lines) {
        int startLine = lines.peekLineNumber();
        String header = lines.next();
        String trimmed = header.trim();
        if (trimmed.startsWith("----")) {
            throwTopLevelDashDash(sourceName, startLine, header);
        }
        String[] tokens = splitTokens(header);
        if (tokens.length == 0) {
            throw new ParseException(sourceName, startLine, "empty record header");
        }

        String kind = tokens[0];
        return switch (kind) {
            case "statement" -> parseStatement(sourceName, startLine, tokens, header, lines);
            case "query" -> parseQuery(sourceName, startLine, tokens, lines);
            case "skipif" -> parseSkipIf(
                    sourceName, startLine, splitTokens(stripTrailingHashComment(header)));
            case "onlyif" -> parseOnlyIf(
                    sourceName, startLine, splitTokens(stripTrailingHashComment(header)));
            case "hash-threshold" -> parseHashThreshold(sourceName, startLine, tokens);
            case "halt" -> parseHalt(sourceName, startLine, tokens);
            default -> throw new ParseException(
                    sourceName, startLine, "unknown record type: " + kind);
        };
    }

    /**
     * Top-level lines beginning with {@code ----} are never records: exact
     * {@code ----} is an expected-results separator; {@code ---- separator …}
     * is a removed expectation-header form (use query-head {@code separator}).
     */
    private static void throwTopLevelDashDash(String sourceName, int startLine, String header) {
        String trimmed = header.trim();
        if (trimmed.equals("----")) {
            throw new ParseException(
                    sourceName,
                    startLine,
                    "---- is an expected-results separator, not a top-level record");
        }
        if (looksLikeRemovedSeparatorExpectationHeader(stripLeadingWhitespace(header))) {
            throw new ParseException(
                    sourceName,
                    startLine,
                    "---- separator was removed; declare separator <delim> on the query header,"
                            + " not as a top-level record");
        }
        throw new ParseException(
                sourceName,
                startLine,
                "invalid ---- directive (expected exact '----' as a query expectation header): "
                        + trimmed);
    }

    private static StatementRecord parseStatement(
            String sourceName, int startLine, String[] tokens, String headerLine, LineBuffer lines) {
        if (tokens.length < 2) {
            throw new ParseException(
                    sourceName, startLine, "statement requires at least one expectation token (ok|error)");
        }
        StatementExpectation expectation = switch (tokens[1]) {
            case "ok" -> {
                if (tokens.length > 2) {
                    throw new ParseException(
                            sourceName, startLine,
                            "statement ok does not take additional operands");
                }
                yield StatementExpectation.OK;
            }
            case "error" -> StatementExpectation.ERROR;
            default -> throw new ParseException(
                    sourceName, startLine, "unknown statement expectation: " + tokens[1]);
        };
        String expectedErrorMsg = null;
        int errorMsgStartColumn = -1;
        if (expectation == StatementExpectation.ERROR && tokens.length > 2) {
            String keyword = "error";
            int keywordEnd = indexOfToken(headerLine, keyword, 0);
            if (keywordEnd >= 0) {
                String raw = headerLine.substring(keywordEnd).trim();
                if (!raw.isEmpty()) {
                    expectedErrorMsg = raw;
                    errorMsgStartColumn = findMsgStartColumn(headerLine, keywordEnd);
                }
            }
        }
        String sql = readSqlBody(sourceName, startLine, lines);
        return new StatementRecord(
                sql, expectation, expectedErrorMsg, new SourceLocation(sourceName, startLine), errorMsgStartColumn);
    }

    private static int findMsgStartColumn(String headerLine, int keywordEnd) {
        int pos = keywordEnd;
        while (pos < headerLine.length() && Character.isWhitespace(headerLine.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private static QueryRecord parseQuery(
            String sourceName, int startLine, String[] tokens, LineBuffer lines) {
        if (tokens.length < 2) {
            throw new ParseException(sourceName, startLine, "query requires a type signature");
        }
        List<ColumnType> typeSignature = parseTypeSignature(sourceName, startLine, tokens[1]);
        SortMode sortMode = SortMode.NOSORT;
        Optional<String> label = Optional.empty();
        Optional<String> columnSeparator = Optional.empty();
        int index = 2;
        if (index < tokens.length) {
            SortMode parsed = parseSortMode(tokens[index]);
            if (parsed != null) {
                sortMode = parsed;
                index++;
            }
        }
        int remaining = tokens.length - index;
        if (remaining == 1) {
            label = Optional.of(tokens[index]);
        } else if (remaining == 2) {
            if (!tokens[index].equals("separator")) {
                throw new ParseException(
                        sourceName, startLine, "unexpected tokens in query header after label");
            }
            columnSeparator = Optional.of(tokens[index + 1]);
        } else if (remaining == 3) {
            if (!tokens[index + 1].equals("separator")) {
                throw new ParseException(
                        sourceName, startLine, "unexpected tokens in query header after label");
            }
            label = Optional.of(tokens[index]);
            columnSeparator = Optional.of(tokens[index + 2]);
        } else if (remaining > 3) {
            throw new ParseException(
                    sourceName,
                    startLine,
                    "unexpected tokens in query header after separator <delim>");
        }

        List<String> sqlLines = new ArrayList<>();
        boolean hasExpected = false;
        List<String> expected = List.of();
        int expectedHeaderLine = 0;

        while (lines.hasNext()) {
            String line = lines.peek();
            if (line.isEmpty()) {
                break;
            }
            if (isExpectationHeaderCandidate(line)) {
                requireExactExpectationHeader(sourceName, lines.peekLineNumber(), line);
                expectedHeaderLine = lines.peekLineNumber();
                lines.next();
                hasExpected = true;
                expected = readExpectedResults(lines);
                break;
            }
            // Next record header without a blank separator ends this query (execute-only or SQL done).
            if (isRecordStart(line) && !sqlLines.isEmpty()) {
                break;
            }
            sqlLines.add(lines.next());
        }

        if (sqlLines.isEmpty()) {
            throw new ParseException(sourceName, startLine, "query is missing SQL body");
        }

        int expectedBodyEndLine = hasExpected ? expectedHeaderLine + expected.size() : 0;

        return new QueryRecord(
                typeSignature,
                sortMode,
                label,
                String.join("\n", sqlLines),
                hasExpected,
                expected,
                columnSeparator,
                new SourceLocation(sourceName, startLine),
                expectedHeaderLine,
                expectedBodyEndLine);
    }

    private static boolean isExpectationHeaderCandidate(String rawLine) {
        return stripLeadingWhitespace(rawLine).startsWith("----");
    }

    /**
     * Accepts only trim-exact {@code ----}. Removed {@code ---- separator …} and other
     * {@code ----…} forms raise a readable {@link ParseException}.
     */
    private static void requireExactExpectationHeader(String sourceName, int lineNumber, String rawLine) {
        String leadingStripped = stripLeadingWhitespace(rawLine);
        String trimmed = rawLine.trim();
        if (trimmed.equals("----")) {
            return;
        }
        if (looksLikeRemovedSeparatorExpectationHeader(leadingStripped)) {
            throw new ParseException(
                    sourceName,
                    lineNumber,
                    "---- separator was removed; declare separator <delim> on the query header"
                            + " instead: "
                            + trimmed);
        }
        throw new ParseException(
                sourceName,
                lineNumber,
                "invalid ---- directive (expected exact '----'): " + trimmed);
    }

    /** True when the line matches {@code ----} + optional blank + {@code separator}… (removed form). */
    private static boolean looksLikeRemovedSeparatorExpectationHeader(String line) {
        if (!line.startsWith("----")) {
            return false;
        }
        int i = 4;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        final String keyword = "separator";
        return i + keyword.length() <= line.length()
                && line.regionMatches(i, keyword, 0, keyword.length());
    }

    private static String stripLeadingWhitespace(String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return line.substring(i);
    }

    private static SkipIfRecord parseSkipIf(String sourceName, int startLine, String[] tokens) {
        if (tokens.length != 2) {
            throw new ParseException(sourceName, startLine, "skipif requires a database name");
        }
        return new SkipIfRecord(tokens[1], new SourceLocation(sourceName, startLine));
    }

    private static OnlyIfRecord parseOnlyIf(String sourceName, int startLine, String[] tokens) {
        if (tokens.length != 2) {
            throw new ParseException(sourceName, startLine, "onlyif requires a database name");
        }
        return new OnlyIfRecord(tokens[1], new SourceLocation(sourceName, startLine));
    }

    /** Strips a trailing {@code # …} comment from an {@code onlyif}/{@code skipif} header. */
    private static String stripTrailingHashComment(String line) {
        int hash = line.indexOf('#');
        if (hash < 0) {
            return line;
        }
        return line.substring(0, hash);
    }

    private static HashThresholdRecord parseHashThreshold(
            String sourceName, int startLine, String[] tokens) {
        if (tokens.length != 2) {
            throw new ParseException(sourceName, startLine, "hash-threshold requires an integer operand");
        }
        try {
            int threshold = Integer.parseInt(tokens[1]);
            return new HashThresholdRecord(threshold, new SourceLocation(sourceName, startLine));
        } catch (NumberFormatException ex) {
            throw new ParseException(
                    sourceName, startLine, "hash-threshold operand is not an integer: " + tokens[1]);
        }
    }

    private static HaltRecord parseHalt(String sourceName, int startLine, String[] tokens) {
        if (tokens.length != 1) {
            throw new ParseException(sourceName, startLine, "halt does not take operands");
        }
        return new HaltRecord(new SourceLocation(sourceName, startLine));
    }

    private static String readSqlBody(String sourceName, int startLine, LineBuffer lines) {
        List<String> sqlLines = new ArrayList<>();
        while (lines.hasNext()) {
            String line = lines.peek();
            if (line.isEmpty()) {
                break;
            }
            if (isRecordStart(line) && !sqlLines.isEmpty()) {
                break;
            }
            sqlLines.add(lines.next());
        }
        if (sqlLines.isEmpty()) {
            throw new ParseException(sourceName, startLine, "statement is missing SQL body");
        }
        return String.join("\n", sqlLines);
    }

    private static List<String> readExpectedResults(LineBuffer lines) {
        List<String> expected = new ArrayList<>();
        while (lines.hasNext()) {
            String line = lines.peek();
            if (line.isEmpty()) {
                break;
            }
            expected.add(lines.next());
        }
        return List.copyOf(expected);
    }

    private static List<ColumnType> parseTypeSignature(
            String sourceName, int startLine, String signature) {
        if (signature.isEmpty()) {
            throw new ParseException(sourceName, startLine, "type signature must not be empty");
        }
        List<ColumnType> types = new ArrayList<>(signature.length());
        for (int i = 0; i < signature.length(); i++) {
            char code = signature.charAt(i);
            ColumnType type = ColumnType.fromCode(code);
            if (type == null) {
                throw new ParseException(
                        sourceName,
                        startLine,
                        "illegal type signature character '" + code + "' in \"" + signature + "\"");
            }
            types.add(type);
        }
        return List.copyOf(types);
    }

    private static SortMode parseSortMode(String token) {
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "nosort" -> SortMode.NOSORT;
            case "rowsort" -> SortMode.ROWSORT;
            case "valuesort" -> SortMode.VALUESORT;
            default -> null;
        };
    }

    private static boolean isRecordStart(String line) {
        if (line.isEmpty() || line.startsWith("#")) {
            return false;
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("----")) {
            // Expectation headers are handled inside parseQuery; other ----… fail at top level.
            return true;
        }
        int space = indexOfWhitespace(line);
        String first = space < 0 ? line : line.substring(0, space);
        return switch (first) {
            case "statement", "query", "skipif", "onlyif", "hash-threshold", "halt" -> true;
            default -> false;
        };
    }

    private static String[] splitTokens(String line) {
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new String[0];
        }
        return trimmed.split("\\s+");
    }

    private static int indexOfWhitespace(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (Character.isWhitespace(line.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Finds the end index (past the last character) of the {@code n}th occurrence of
     * {@code token} in {@code line}, where occurrences are separated by whitespace.
     * Returns -1 if not found.
     */
    private static int indexOfToken(String line, String token, int n) {
        int pos = 0;
        int found = 0;
        while (pos < line.length()) {
            while (pos < line.length() && Character.isWhitespace(line.charAt(pos))) {
                pos++;
            }
            if (pos >= line.length()) {
                break;
            }
            int start = pos;
            while (pos < line.length() && !Character.isWhitespace(line.charAt(pos))) {
                pos++;
            }
            String tok = line.substring(start, pos);
            if (tok.equals(token)) {
                found++;
                if (found == n + 1) {
                    return pos;
                }
            }
        }
        return -1;
    }

    /** Mutable cursor over source lines with 1-based line numbers. */
    private static final class LineBuffer {
        private final List<String> lines;
        private int index;

        private LineBuffer(List<String> lines) {
            this.lines = lines;
            this.index = 0;
        }

        static LineBuffer from(String content) {
            return new LineBuffer(splitPreserveAllLines(content));
        }

        private static List<String> splitPreserveAllLines(String content) {
            List<String> result = new ArrayList<>();
            if (content.isEmpty()) {
                return result;
            }
            int begin = 0;
            for (int i = 0; i < content.length(); i++) {
                if (content.charAt(i) == '\n') {
                    result.add(stripTrailingCr(content.substring(begin, i)));
                    begin = i + 1;
                }
            }
            if (begin < content.length()) {
                result.add(stripTrailingCr(content.substring(begin)));
            }
            return result;
        }

        private static String stripTrailingCr(String line) {
            if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') {
                return line.substring(0, line.length() - 1);
            }
            return line;
        }

        boolean hasNext() {
            return index < lines.size();
        }

        String peek() {
            return lines.get(index);
        }

        int peekLineNumber() {
            return index + 1;
        }

        String next() {
            return lines.get(index++);
        }
    }
}
