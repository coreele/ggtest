package com.ggtest.model;

/**
 * Closed set of sqllogictest record types produced by the parser. Downstream
 * slices ({@code normalize}, {@code runner}) consume records through this sealed
 * hierarchy and can exhaustively {@code switch} over the permitted variants.
 */
public sealed interface SqlTestRecord
        permits StatementRecord,
                QueryRecord,
                SkipIfRecord,
                OnlyIfRecord,
                HashThresholdRecord,
                HaltRecord {

    /** Source location (name + starting line) where this record begins. */
    SourceLocation location();
}
