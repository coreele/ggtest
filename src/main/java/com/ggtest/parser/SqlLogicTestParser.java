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

    private static final String DEFAULT_COLUMN_SEPARATOR = " ";

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
            case "statement" -> parseStatement(sourceName, startLine, tokens, lines);
            case "query" -> parseQuery(sourceName, startLine, tokens, lines);
            case "skipif" -> parseSkipIf(sourceName, startLine, tokens);
            case "onlyif" -> parseOnlyIf(sourceName, startLine, tokens);
            case "hash-threshold" -> parseHashThreshold(sourceName, startLine, tokens);
            case "halt" -> parseHalt(sourceName, startLine, tokens);
            default -> throw new ParseException(
                    sourceName, startLine, "unknown record type: " + kind);
        };
    }

    /**
     * Top-level lines beginning with {@code ----} are never records: exact
     * {@code ----} is an expected-results separator; {@code ---- separator …}
     * belongs on a query expectation header (R1).
     */
    private static void throwTopLevelDashDash(String sourceName, int startLine, String header) {
        String trimmed = header.trim();
        if (trimmed.equals("----")) {
            throw new ParseException(
                    sourceName,
                    startLine,
                    "---- is an expected-results separator, not a top-level record");
        }
        if (looksLikeSeparatorExpectationHeader(stripLeadingWhitespace(header))) {
            throw new ParseException(
                    sourceName,
                    startLine,
                    "---- separator must be a query expectation header, not a top-level record");
        }
        throw new ParseException(
                sourceName,
                startLine,
                "invalid ---- directive (expected '----' or '---- separator <delim>' as a query"
                        + " expectation header): "
                        + trimmed);
    }

    private static StatementRecord parseStatement(
            String sourceName, int startLine, String[] tokens, LineBuffer lines) {
        if (tokens.length != 2) {
            throw new ParseException(
                    sourceName, startLine, "statement requires exactly one expectation token (ok|error)");
        }
        StatementExpectation expectation = switch (tokens[1]) {
            case "ok" -> StatementExpectation.OK;
            case "error" -> StatementExpectation.ERROR;
            default -> throw new ParseException(
                    sourceName, startLine, "unknown statement expectation: " + tokens[1]);
        };
        String sql = readSqlBody(sourceName, startLine, lines);
        return new StatementRecord(sql, expectation, new SourceLocation(sourceName, startLine));
    }

    private static QueryRecord parseQuery(
            String sourceName, int startLine, String[] tokens, LineBuffer lines) {
        if (tokens.length < 2) {
            throw new ParseException(sourceName, startLine, "query requires a type signature");
        }
        List<ColumnType> typeSignature = parseTypeSignature(sourceName, startLine, tokens[1]);
        SortMode sortMode = SortMode.NOSORT;
        Optional<String> label = Optional.empty();
        int index = 2;
        if (index < tokens.length) {
            SortMode parsed = parseSortMode(tokens[index]);
            if (parsed != null) {
                sortMode = parsed;
                index++;
            }
        }
        if (index < tokens.length) {
            label = Optional.of(tokens[index]);
            index++;
        }
        if (index < tokens.length) {
            throw new ParseException(
                    sourceName, startLine, "unexpected tokens in query header after label");
        }

        List<String> sqlLines = new ArrayList<>();
        boolean hasExpected = false;
        List<String> expected = List.of();
        String columnSeparator = DEFAULT_COLUMN_SEPARATOR;
        boolean explicitColumnSeparator = false;

        while (lines.hasNext()) {
            String line = lines.peek();
            if (line.isEmpty()) {
                break;
            }
            Optional<ExpectationHeader> header = tryParseExpectationHeader(sourceName, lines.peekLineNumber(), line);
            if (header.isPresent()) {
                lines.next();
                hasExpected = true;
                columnSeparator = header.get().columnSeparator();
                explicitColumnSeparator = header.get().explicit();
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

        return new QueryRecord(
                typeSignature,
                sortMode,
                label,
                String.join("\n", sqlLines),
                hasExpected,
                expected,
                columnSeparator,
                explicitColumnSeparator,
                new SourceLocation(sourceName, startLine));
    }

    /**
     * Parses a query expectation header line: exact {@code ----} (default space) or
     * {@code ---- separator <delim>} (explicit). Other {@code ----…} lines fail.
     */
    private static Optional<ExpectationHeader> tryParseExpectationHeader(
            String sourceName, int lineNumber, String rawLine) {
        String leadingStripped = stripLeadingWhitespace(rawLine);
        if (!leadingStripped.startsWith("----")) {
            return Optional.empty();
        }
        String trimmed = rawLine.trim();
        if (trimmed.equals("----")) {
            return Optional.of(new ExpectationHeader(DEFAULT_COLUMN_SEPARATOR, false));
        }
        return Optional.of(parseSeparatorExpectationHeader(sourceName, lineNumber, leadingStripped));
    }

    private static ExpectationHeader parseSeparatorExpectationHeader(
            String sourceName, int lineNumber, String line) {
        // line starts with ---- (caller stripped leading whitespace) but is not exactly ----
        int i = 4;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        final String keyword = "separator";
        if (i + keyword.length() > line.length()
                || !line.regionMatches(i, keyword, 0, keyword.length())) {
            throw new ParseException(
                    sourceName,
                    lineNumber,
                    "invalid ---- directive (expected '----' or '---- separator <delim>'): "
                            + line.trim());
        }
        i += keyword.length();
        // After keyword: optional one leading whitespace, then <delim> to end of line.
        // Do not use splitTokens — delim is the literal remainder (may contain spaces).
        if (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        String delim = line.substring(i);
        if (delim.isEmpty()) {
            throw new ParseException(
                    sourceName, lineNumber, "---- separator requires a non-empty delimiter");
        }
        return new ExpectationHeader(delim, true);
    }

    /** True when the line matches {@code ----} + optional blank + {@code separator}… (for messages). */
    private static boolean looksLikeSeparatorExpectationHeader(String line) {
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

    private record ExpectationHeader(String columnSeparator, boolean explicit) {}

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
