package com.ggtest.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResultComparerTest {

    @Test
    void defaultHashThresholdIsEight() {
        assertEquals(8, ResultComparer.DEFAULT_HASH_THRESHOLD);
    }

    @Test
    void thresholdZeroForcesFullTextEvenWhenManyValues() {
        List<ColumnType> types = List.of(ColumnType.INTEGER);
        List<List<String>> rows = new java.util.ArrayList<>();
        StringBuilder expected = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            rows.add(List.of(Integer.toString(i)));
            expected.append(i).append('\n');
        }
        var result = ResultComparer.compare(
                types, SortMode.NOSORT, 0, expected.toString(), rows);
        assertTrue(result.passed());
        assertEquals(10, result.actualView().size());
    }

    @Test
    void failureDiffSummaryIsNonEmptyGitStyle() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER),
                SortMode.NOSORT,
                "1\n",
                List.of(List.of("2")));
        assertFalse(result.passed());
        assertFalse(result.diffSummary().isBlank());
        assertTrue(result.diffSummary().contains("-   1"));
        assertTrue(result.diffSummary().contains("+   2"));
        assertFalse(result.diffSummary().contains("expected ("));
        assertFalse(result.diffSummary().contains("actual ("));
    }

    @Test
    void gitDiffKeepsUnchangedContextLines() {
        var result = ResultComparer.compare(
                List.of(ColumnType.TEXT),
                SortMode.NOSORT,
                "apple\nbananad\ncherry\n",
                List.of(List.of("apple"), List.of("banana"), List.of("cherry")));
        assertFalse(result.passed());
        String diff = result.diffSummary();
        assertTrue(diff.contains("    apple"));
        assertTrue(diff.contains("-   bananad"));
        assertTrue(diff.contains("+   banana"));
        assertTrue(diff.contains("    cherry"));
    }

    // --- row-wise via query-head separator; value-per-line is default ---- ---

    @Test
    void p0_3_defaultSpaceRowWiseAbolished_singleValueFails() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                "1 2 3\n",
                List.of(List.of("1", "2", "3")));
        assertFalse(result.passed(), "bare ---- treats '1 2 3' as one cell");
        assertEquals(List.of("1 2 3"), result.expectedView());
    }

    @Test
    void p0_3_consecutiveSpaces_noLongerRowWiseEmptyTokens() {
        var result = ResultComparer.compare(
                List.of(ColumnType.TEXT, ColumnType.TEXT, ColumnType.TEXT),
                SortMode.NOSORT,
                "a  c\n",
                List.of(List.of("a", "", "c")));
        assertFalse(result.passed());
        assertEquals(List.of("a  c"), result.expectedView());
    }

    @Test
    void p0_4_select4Shape_spacedTextValuePerLinePasses() {
        var result = ResultComparer.compare(
                List.of(
                        ColumnType.INTEGER,
                        ColumnType.TEXT,
                        ColumnType.INTEGER,
                        ColumnType.INTEGER),
                SortMode.ROWSORT,
                "51732\ntable tn7 row 92\n511\n84280\n",
                List.of(List.of("51732", "table tn7 row 92", "511", "84280")));
        assertTrue(result.passed(), result.diffSummary());
        assertFalse(result.diffSummary().toLowerCase().contains("mixed expected line shapes"));
    }

    @Test
    void p0_2_targetWriting_iitPipeBareTextPasses() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.TEXT),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                "1 | 1 | hello world\n",
                List.of(List.of("1", "1", "hello world")));
        assertTrue(result.passed(), result.diffSummary());
        assertEquals(List.of("1", "1", "hello world"), result.expectedView());
    }

    @Test
    void p0_4_declaredTrim_spacesAroundPipePass() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                "1 | 2 | 3\n",
                List.of(List.of("1", "2", "3")));
        assertTrue(result.passed(), result.diffSummary());
    }

    @Test
    void p0_5_cellContainingSeparator_failsUnlessDifferentSepOrValuePerLine() {
        List<ColumnType> types = List.of(ColumnType.TEXT, ColumnType.TEXT);
        List<List<String>> actual = List.of(List.of("a|b", "c"));

        var withPipe = ResultComparer.compare(
                types,
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                "a|b|c\n",
                actual);
        assertFalse(withPipe.passed());
        String msg = withPipe.diffSummary().toLowerCase();
        assertTrue(msg.contains("token") || msg.contains("column") || msg.contains("2"), withPipe.diffSummary());

        var withComma = ResultComparer.compare(
                types,
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of(","),
                "a|b,c\n",
                actual);
        assertTrue(withComma.passed(), withComma.diffSummary());

        var valuePerLine = ResultComparer.compare(
                types, SortMode.NOSORT, "a|b\nc\n", actual);
        assertTrue(valuePerLine.passed(), valuePerLine.diffSummary());
    }

    @Test
    void p1_4_literalQuotesAreCellContent_notUnquoted() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.TEXT),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                "1 | 1 | 'hello world'\n",
                List.of(List.of("1", "1", "hello world")));
        assertFalse(result.passed(), "quoted token must not unquote to match bare actual");
        assertEquals(List.of("1", "1", "'hello world'"), result.expectedView());
    }

    @Test
    void p0_6_valuePerLineStillPasses() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                "1\n2\n3\n",
                List.of(List.of("1", "2", "3")));
        assertTrue(result.passed(), result.diffSummary());
    }

    @Test
    void p0_8_hashFormUnchanged_withAndWithoutDeclaredSeparator() {
        List<String> values = List.of("1", "2", "3");
        String hashLine = ResultHasher.hashForm(values);

        var hashNoSep = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                1,
                Optional.empty(),
                hashLine,
                List.of(List.of("1", "2", "3")));
        assertTrue(hashNoSep.passed(), hashNoSep.diffSummary());
        assertEquals(List.of(hashLine), hashNoSep.actualView());

        var hashWithSep = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                1,
                Optional.of("|"),
                hashLine,
                List.of(List.of("1", "2", "3")));
        assertTrue(hashWithSep.passed(), hashWithSep.diffSummary());

        var declaredPlain = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                0,
                Optional.of("|"),
                "1|2|3\n",
                List.of(List.of("1", "2", "3")));
        assertTrue(declaredPlain.passed(), declaredPlain.diffSummary());
        assertEquals(hashLine, ResultHasher.hashForm(declaredPlain.expectedView()));
    }

    @Test
    void p0_8_rowWiseRowsortPassesNosortFailsWithDiff() {
        List<ColumnType> types = List.of(ColumnType.INTEGER, ColumnType.INTEGER);
        List<List<String>> actual = List.of(List.of("2", "1"), List.of("1", "2"));
        String expected = "1|2\n2|1\n";

        var rowsort = ResultComparer.compare(
                types,
                SortMode.ROWSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                expected,
                actual);
        assertTrue(rowsort.passed(), rowsort.diffSummary());

        var nosort = ResultComparer.compare(
                types,
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                expected,
                actual);
        assertFalse(nosort.passed());
        assertFalse(nosort.diffSummary().isBlank());
        assertTrue(nosort.diffSummary().contains("-   ") || nosort.diffSummary().contains("+   "));
    }

    @Test
    void p1_2_rowWise_emptyTokenAndEmptyLiteral_alignWithTextEmpty() {
        var emptyToken = ResultComparer.compare(
                List.of(ColumnType.TEXT, ColumnType.TEXT, ColumnType.TEXT),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                "a||c\n",
                List.of(List.of("a", "", "c")));
        assertTrue(emptyToken.passed(), emptyToken.diffSummary());

        var emptyLiteral = ResultComparer.compare(
                List.of(ColumnType.TEXT, ColumnType.TEXT),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                "x|(empty)\n",
                List.of(List.of("x", "")));
        assertTrue(emptyLiteral.passed(), emptyLiteral.diffSummary());
    }

    @Test
    void p0_5_mixedTokenCounts_returnsFailedCompareNotThrow() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                Optional.of("|"),
                "1|2|3\n4|5\n",
                List.of(List.of("1", "2", "3"), List.of("4", "5", "6")));
        assertFalse(result.passed());
        String msg = result.diffSummary().toLowerCase();
        assertTrue(msg.contains("2") || msg.contains("line"), result.diffSummary());
        assertTrue(msg.contains("3") || msg.contains("token") || msg.contains("column"), result.diffSummary());
        assertFalse(msg.contains("mixed expected line shapes"));
    }

    @Test
    void singleColumn_valuePerLinePasses() {
        var valuePerLine = ResultComparer.compare(
                List.of(ColumnType.INTEGER), SortMode.NOSORT, "7\n", List.of(List.of("7")));
        assertTrue(valuePerLine.passed());
    }
}
