package com.ggtest.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ggtest.model.ColumnType;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TypeSignatureInfererTest {

    @Test
    void infersIntegerRealTextColumns() {
        List<List<String>> rows = List.of(
                List.of("1", "1.5", "abc"),
                List.of("2", "2.5", "def"));
        assertEquals(List.of(ColumnType.INTEGER, ColumnType.REAL, ColumnType.TEXT),
                TypeSignatureInferer.infer(rows));
    }

    @Test
    void infersSingleTextColumn() {
        assertEquals(List.of(ColumnType.TEXT),
                TypeSignatureInferer.infer(List.of(List.of("apple"), List.of("banana"))));
    }

    @Test
    void nullsDoNotConstrainType() {
        List<List<String>> rows = Arrays.asList(
                Arrays.asList("1"),
                Arrays.asList((String) null));
        assertEquals(List.of(ColumnType.INTEGER), TypeSignatureInferer.infer(rows));
    }

    @Test
    void allNullColumnDefaultsToText() {
        List<List<String>> rows = Arrays.asList(
                Arrays.asList((String) null),
                Arrays.asList((String) null));
        assertEquals(List.of(ColumnType.TEXT), TypeSignatureInferer.infer(rows));
    }

    @Test
    void emptyResultSetYieldsEmptySignature() {
        assertEquals(List.of(), TypeSignatureInferer.infer(List.of()));
    }

    @Test
    void mixedIntegerRealDefaultsToReal() {
        assertEquals(List.of(ColumnType.REAL),
                TypeSignatureInferer.infer(List.of(List.of("1"), List.of("1.5"))));
    }

    @Test
    void negativeAndLargeIntegersClassifyAsInteger() {
        assertEquals(List.of(ColumnType.INTEGER),
                TypeSignatureInferer.infer(List.of(List.of("-42"), List.of("0"))));
    }
}
