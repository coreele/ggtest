package com.ggtest.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ggtest.model.ColumnType;
import com.ggtest.model.SortMode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Acceptance coverage for Spec P0-2, P0-4, P0-5, P1-3.
 */
class NormalizeAcceptanceTest {

    @Test
    void p0_2_hashMatchesSelect1CorpusExcerpt() throws IOException {
        String fixture = readFixture("fixtures/normalize/p0-2-select1-hash.txt");
        List<String> values = extractValuesSection(fixture);
        assertEquals(30, values.size());
        assertEquals("3c13dee48d9356ae19af2515e05e6b54", ResultHasher.md5Hex(values));

        List<List<String>> rows = new ArrayList<>();
        for (String v : values) {
            rows.add(List.of(v));
        }
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER),
                SortMode.NOSORT,
                8,
                "30 values hashing to 3c13dee48d9356ae19af2515e05e6b54",
                rows);
        assertTrue(result.passed(), result.diffSummary());
    }

    @Test
    void p0_4_integerNullPasses() {
        var result = ResultComparer.compare(
                List.of(ColumnType.INTEGER),
                SortMode.NOSORT,
                "NULL\n",
                List.of(singletonNullRow()));
        assertTrue(result.passed(), result.diffSummary());
    }

    @Test
    void p0_4_realThreeDecimalsPasses() {
        var result = ResultComparer.compare(
                List.of(ColumnType.REAL),
                SortMode.NOSORT,
                "3.142\n",
                List.of(List.of("3.14159")));
        assertTrue(result.passed(), result.diffSummary());
    }

    @Test
    void p0_4_textEmptyPasses() {
        var result = ResultComparer.compare(
                List.of(ColumnType.TEXT),
                SortMode.NOSORT,
                "(empty)\n",
                List.of(List.of("")));
        assertTrue(result.passed(), result.diffSummary());
    }

    @Test
    void p0_5_rowsortPassesNosortFailsWithDiff() {
        List<ColumnType> types = List.of(ColumnType.INTEGER, ColumnType.INTEGER);
        // Actual row order differs from expected line order
        List<List<String>> actual = List.of(
                List.of("2", "1"),
                List.of("1", "2"));
        String expected = "1\n2\n2\n1\n";

        var rowsort = ResultComparer.compare(types, SortMode.ROWSORT, expected, actual);
        assertTrue(rowsort.passed(), rowsort.diffSummary());

        var nosort = ResultComparer.compare(types, SortMode.NOSORT, expected, actual);
        assertFalse(nosort.passed());
        assertFalse(nosort.diffSummary().isBlank());
        assertTrue(nosort.diffSummary().contains("expected"));
        assertTrue(nosort.diffSummary().contains("actual"));
    }

    @Test
    void p1_3_valuesortPasses() {
        List<ColumnType> types = List.of(ColumnType.INTEGER, ColumnType.INTEGER);
        List<List<String>> actual = List.of(
                List.of("2", "1"),
                List.of("4", "3"));
        // Full-value sorted expectation
        String expected = "1\n2\n3\n4\n";
        var result = ResultComparer.compare(types, SortMode.VALUESORT, expected, actual);
        assertTrue(result.passed(), result.diffSummary());
    }

    private static List<String> singletonNullRow() {
        List<String> row = new ArrayList<>(1);
        row.add(null);
        return row;
    }

    private static String readFixture(String path) throws IOException {
        try (InputStream in = NormalizeAcceptanceTest.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IOException("missing fixture: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> extractValuesSection(String fixture) {
        String marker = "values:\n";
        int idx = fixture.indexOf(marker);
        if (idx < 0) {
            throw new IllegalArgumentException("fixture missing values section");
        }
        String body = fixture.substring(idx + marker.length()).stripTrailing();
        if (body.isEmpty()) {
            return List.of();
        }
        return List.of(body.split("\n"));
    }
}
