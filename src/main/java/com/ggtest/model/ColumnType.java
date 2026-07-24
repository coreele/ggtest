package com.ggtest.model;

/**
 * A single column type in a query type signature. Each character of a type
 * signature maps to exactly one {@code ColumnType}.
 */
public enum ColumnType {
    INTEGER('I'),
    TEXT('T'),
    REAL('R');

    private final char code;

    ColumnType(char code) {
        this.code = code;
    }

    /** Returns the single-character signature code ({@code I}, {@code T} or {@code R}). */
    public char code() {
        return code;
    }

    /**
     * Maps a signature character to its {@code ColumnType}.
     *
     * @param code one of {@code I}, {@code T}, {@code R}
     * @return the matching {@code ColumnType}, or {@code null} when the character is not a valid code
     */
    public static ColumnType fromCode(char code) {
        for (ColumnType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
