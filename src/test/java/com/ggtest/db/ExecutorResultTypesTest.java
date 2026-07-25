package com.ggtest.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExecutorResultTypesTest {

    @Test
    void statementOkCarriesNoErrorSummary() {
        StatementResult result = StatementResult.ok();

        assertTrue(result.succeeded());
        assertEquals("", result.errorSummary());
    }

    @Test
    void statementFailureKeepsErrorSummary() {
        StatementResult result = StatementResult.failed("no such table: t1");

        assertFalse(result.succeeded());
        assertEquals("no such table: t1", result.errorSummary());
    }

    @Test
    void statementFailureNormalizesMissingSummary() {
        assertEquals("", StatementResult.failed(null).errorSummary());
    }

    @Test
    void querySuccessPreservesSqlNullValues() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(Arrays.asList("1", null));

        QueryResult result = QueryResult.succeeded(rows);

        assertTrue(result.succeeded());
        assertEquals(1, result.rows().size());
        assertEquals("1", result.rows().get(0).get(0));
        assertNull(result.rows().get(0).get(1));
        assertEquals("", result.errorSummary());
    }

    @Test
    void querySuccessCopiesRowsDefensively() {
        List<List<String>> rows = new ArrayList<>();
        rows.add(new ArrayList<>(List.of("1")));

        QueryResult result = QueryResult.succeeded(rows);
        rows.clear();

        assertEquals(1, result.rows().size());
        assertThrows(UnsupportedOperationException.class, () -> result.rows().add(List.of("2")));
        assertThrows(UnsupportedOperationException.class, () -> result.rows().get(0).add("2"));
    }

    @Test
    void queryFailureHasNoRows() {
        QueryResult result = QueryResult.failed("syntax error");

        assertFalse(result.succeeded());
        assertTrue(result.rows().isEmpty());
        assertEquals("syntax error", result.errorSummary());
    }

    @Test
    void fatalDatabaseExceptionIsUncheckedAndKeepsCause() {
        Throwable cause = new IllegalStateException("connection closed");

        FatalDatabaseException exception = new FatalDatabaseException("connection lost", cause);

        assertTrue(exception instanceof RuntimeException);
        assertEquals("connection lost", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
