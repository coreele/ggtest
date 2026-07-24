package com.ggtest.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResultSorterTest {

    private static final List<ColumnType> II = List.of(ColumnType.INTEGER, ColumnType.INTEGER);

    @Test
    void nosortPreservesRowMajorOrder() {
        List<List<String>> rows = List.of(
                List.of("2", "1"),
                List.of("1", "3"));
        assertEquals(List.of("2", "1", "1", "3"),
                ResultSorter.normalizeAndSort(II, SortMode.NOSORT, rows));
    }

    @Test
    void rowsortOrdersByNormalizedRow() {
        List<List<String>> rows = List.of(
                List.of("2", "1"),
                List.of("1", "3"),
                List.of("1", "2"));
        assertEquals(List.of("1", "2", "1", "3", "2", "1"),
                ResultSorter.normalizeAndSort(II, SortMode.ROWSORT, rows));
    }

    @Test
    void valuesortFlattensAndSortsAllValues() {
        List<List<String>> rows = List.of(
                List.of("2", "1"),
                List.of("1", "3"));
        assertEquals(List.of("1", "1", "2", "3"),
                ResultSorter.normalizeAndSort(II, SortMode.VALUESORT, rows));
    }

    @Test
    void rowsortUsesNormalizedForms() {
        List<ColumnType> types = List.of(ColumnType.TEXT, ColumnType.REAL);
        List<List<String>> rows = List.of(
                List.of("", "1.5"),
                List.of("a", "0"));
        assertEquals(List.of("(empty)", "1.500", "a", "0.000"),
                ResultSorter.normalizeAndSort(types, SortMode.ROWSORT, rows));
    }
}
