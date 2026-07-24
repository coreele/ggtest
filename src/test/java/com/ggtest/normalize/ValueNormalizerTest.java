package com.ggtest.normalize;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ggtest.model.ColumnType;
import org.junit.jupiter.api.Test;

class ValueNormalizerTest {

    @Test
    void integerNullBecomesNullLiteral() {
        assertEquals("NULL", ValueNormalizer.normalize(ColumnType.INTEGER, null));
    }

    @Test
    void integerFormatsAsDecimal() {
        assertEquals("42", ValueNormalizer.normalize(ColumnType.INTEGER, "42"));
        assertEquals("-7", ValueNormalizer.normalize(ColumnType.INTEGER, "-7"));
    }

    @Test
    void integerUnparseableBecomesZero() {
        assertEquals("0", ValueNormalizer.normalize(ColumnType.INTEGER, "abc"));
        assertEquals("0", ValueNormalizer.normalize(ColumnType.INTEGER, "1.5"));
    }

    @Test
    void realFormatsWithThreeDecimals() {
        assertEquals("1.500", ValueNormalizer.normalize(ColumnType.REAL, "1.5"));
        assertEquals("0.000", ValueNormalizer.normalize(ColumnType.REAL, "0"));
    }

    @Test
    void realNullBecomesNullLiteral() {
        assertEquals("NULL", ValueNormalizer.normalize(ColumnType.REAL, null));
    }

    @Test
    void realUnparseableBecomesZeroPoint() {
        assertEquals("0.000", ValueNormalizer.normalize(ColumnType.REAL, "nan-ish"));
    }

    @Test
    void textEmptyBecomesEmptyToken() {
        assertEquals("(empty)", ValueNormalizer.normalize(ColumnType.TEXT, ""));
    }

    @Test
    void textNullBecomesNullLiteral() {
        assertEquals("NULL", ValueNormalizer.normalize(ColumnType.TEXT, null));
    }

    @Test
    void textReplacesControlAndNonAsciiWithAt() {
        assertEquals("a@b@", ValueNormalizer.normalize(ColumnType.TEXT, "a\nb\u007f"));
        assertEquals("@@", ValueNormalizer.normalize(ColumnType.TEXT, "\u0001\u0080"));
    }

    @Test
    void textKeepsPrintableAscii() {
        assertEquals("Hello!", ValueNormalizer.normalize(ColumnType.TEXT, "Hello!"));
    }
}
