package com.ggtest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ggtest.normalize.ResultComparer;
import org.junit.jupiter.api.Test;

class SqlLogicDefaultsTest {

    @Test
    void defaultColumnSeparatorIsSpace() {
        assertEquals(" ", SqlLogicDefaults.DEFAULT_COLUMN_SEPARATOR);
    }

    @Test
    void resultComparerForwardsDefaultColumnSeparator() {
        assertEquals(
                SqlLogicDefaults.DEFAULT_COLUMN_SEPARATOR,
                ResultComparer.DEFAULT_COLUMN_SEPARATOR);
    }
}
