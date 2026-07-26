package com.ggtest.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.util.List;
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

    // --- P0 row-wise expected (ggtest-rowwise-expected R1/R2; R3 abolished) ---

    @Test
    void p0_1_defaultSpaceRowWiseExpectedPasses() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                "1 2 3\n",
                List.of(List.of("1", "2", "3")));
        assertTrue(result.passed(), result.diffSummary());
        assertEquals(List.of("1", "2", "3"), result.expectedView());
        assertEquals(List.of("1", "2", "3"), result.actualView());
    }

    @Test
    void p0_1_consecutiveSpacesStillEmptyToken_defaultPathNoTrim() {
        var result = ResultComparer.compare(
                List.of(ColumnType.TEXT, ColumnType.TEXT, ColumnType.TEXT),
                SortMode.NOSORT,
                "a  c\n",
                List.of(List.of("a", "", "c")));
        assertTrue(result.passed(), result.diffSummary());
    }

    @Test
    void p0_2_targetWriting_iitPipeBareTextPasses() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.TEXT),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                "|",
                true,
                "1 | 1 | hello world\n",
                List.of(List.of("1", "1", "hello world")));
        assertTrue(result.passed(), result.diffSummary());
        assertEquals(List.of("1", "1", "hello world"), result.expectedView());
    }

    @Test
    void p0_4_explicitTrim_spacesAroundPipePass() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                "|",
                true,
                "1 | 2 | 3\n",
                List.of(List.of("1", "2", "3")));
        assertTrue(result.passed(), result.diffSummary());
    }

    @Test
    void p0_5_cellContainingSeparator_failsUnlessDifferentSepOrValuePerLine() {
        List<ColumnType> types = List.of(ColumnType.TEXT, ColumnType.TEXT);
        List<List<String>> actual = List.of(List.of("a|b", "c"));

        // Still row-wise with S=| → token count ≠ C → value-per-line path → mismatch
        var withPipe = ResultComparer.compare(
                types,
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                "|",
                true,
                "a|b|c\n",
                actual);
        assertFalse(withPipe.passed());
        assertFalse(withPipe.diffSummary().isBlank());

        // Different separator that is not in the cell → pass
        var withComma = ResultComparer.compare(
                types,
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                ",",
                true,
                "a|b,c\n",
                actual);
        assertTrue(withComma.passed(), withComma.diffSummary());

        // Value-per-line → pass
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
                "|",
                true,
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
    void p0_7_hashFormUnchanged_andRowWiseExpandedMd5Matches() {
        List<String> values = List.of("1", "2", "3");
        String hashLine = ResultHasher.hashForm(values);
        var hashCompare = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                1,
                " ",
                false,
                hashLine,
                List.of(List.of("1", "2", "3")));
        assertTrue(hashCompare.passed(), hashCompare.diffSummary());
        assertEquals(List.of(hashLine), hashCompare.actualView());

        var rowWisePlain = ResultComparer.compare(
                List.of(ColumnType.INTEGER, ColumnType.INTEGER, ColumnType.INTEGER),
                SortMode.NOSORT,
                0,
                " ",
                false,
                "1 2 3\n",
                List.of(List.of("1", "2", "3")));
        assertTrue(rowWisePlain.passed(), rowWisePlain.diffSummary());
        assertEquals(hashLine, ResultHasher.hashForm(rowWisePlain.expectedView()));
    }

    @Test
    void p0_8_rowWiseRowsortPassesNosortFailsWithDiff() {
        List<ColumnType> types = List.of(ColumnType.INTEGER, ColumnType.INTEGER);
        List<List<String>> actual = List.of(List.of("2", "1"), List.of("1", "2"));
        String expected = "1 2\n2 1\n";

        var rowsort = ResultComparer.compare(types, SortMode.ROWSORT, expected, actual);
        assertTrue(rowsort.passed(), rowsort.diffSummary());

        var nosort = ResultComparer.compare(types, SortMode.NOSORT, expected, actual);
        assertFalse(nosort.passed());
        assertFalse(nosort.diffSummary().isBlank());
        assertTrue(nosort.diffSummary().contains("-   ") || nosort.diffSummary().contains("+   "));
    }

    @Test
    void rowWise_emptyTokenAndEmptyLiteral_alignWithTextEmpty_explicit() {
        var emptyToken = ResultComparer.compare(
                List.of(ColumnType.TEXT, ColumnType.TEXT, ColumnType.TEXT),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                "|",
                true,
                "a||c\n",
                List.of(List.of("a", "", "c")));
        assertTrue(emptyToken.passed(), emptyToken.diffSummary());

        var emptyLiteral = ResultComparer.compare(
                List.of(ColumnType.TEXT, ColumnType.TEXT),
                SortMode.NOSORT,
                ResultComparer.DEFAULT_HASH_THRESHOLD,
                "|",
                true,
                "x|(empty)\n",
                List.of(List.of("x", "")));
        assertTrue(emptyLiteral.passed(), emptyLiteral.diffSummary());
    }

    @Test
    void mixedTokenCounts_throwsReadableAlignmentFailure() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> ResultComparer.compare(
                        List.of(ColumnType.INTEGER, ColumnType.INTEGER),
                        SortMode.NOSORT,
                        "1 2\n3\n",
                        List.of(List.of("1", "2"), List.of("3", "4"))));
        String msg = ex.getMessage().toLowerCase();
        assertTrue(msg.contains("token") || msg.contains("column") || msg.contains("2"), ex.getMessage());
    }

    @Test
    void singleColumn_rowWiseAndValuePerLineEquivalent() {
        var rowWise = ResultComparer.compare(
                List.of(ColumnType.INTEGER), SortMode.NOSORT, "7\n", List.of(List.of("7")));
        var valuePerLine = ResultComparer.compare(
                List.of(ColumnType.INTEGER), SortMode.NOSORT, "7\n", List.of(List.of("7")));
        assertTrue(rowWise.passed());
        assertTrue(valuePerLine.passed());
    }

}
