package com.ggtest.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void failureDiffSummaryIsNonEmpty() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER),
                SortMode.NOSORT,
                "1\n",
                List.of(List.of("2")));
        assertFalse(result.passed());
        assertFalse(result.diffSummary().isBlank());
        assertTrue(result.diffSummary().contains("expected"));
        assertTrue(result.diffSummary().contains("actual"));
    }
}
