package com.ggtest.model;

/**
 * Sort mode declared on a query record. Only recognition is done by the parser;
 * comparison semantics belong to the {@code normalize} slice.
 */
public enum SortMode {
    NOSORT,
    ROWSORT,
    VALUESORT
}
