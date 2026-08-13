package com.ggtest.runner;

import com.ggtest.model.SourceLocation;
import com.ggtest.model.SqlTestRecord;
import java.util.Objects;
import java.util.Optional;

/**
 * Verdict for a single assertable record, plus the material a report needs to
 * explain a failure or a skip.
 *
 * @param record                    the executed (or skipped) record
 * @param outcome                   the verdict
 * @param failureReason             failure or skip explanation; empty for {@link RecordOutcome#PASSED}
 * @param overrideText              golden text to write back for {@link RecordOutcome#OVERRIDDEN};
 *                                  empty otherwise
 * @param overrideSignature         inferred type signature (I/R/T string) to rewrite into the query
 *                                  header for {@link RecordOutcome#OVERRIDDEN}; empty when the
 *                                  signature is already correct
 * @param overrideAsStatementError  when {@code true}, the record should be rewritten as
 *                                  {@code statement error <overrideText>} (execution-failure golden)
 */
public record RecordResult(
        SqlTestRecord record,
        RecordOutcome outcome,
        String failureReason,
        Optional<String> overrideText,
        Optional<String> overrideSignature,
        boolean overrideAsStatementError) {

    public RecordResult(SqlTestRecord record, RecordOutcome outcome, String failureReason) {
        this(record, outcome, failureReason, Optional.empty(), Optional.empty(), false);
    }

    public RecordResult(SqlTestRecord record, RecordOutcome outcome, String failureReason, Optional<String> overrideText) {
        this(record, outcome, failureReason, overrideText, Optional.empty(), false);
    }

    public RecordResult {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(outcome, "outcome");
        failureReason = failureReason == null ? "" : failureReason;
        overrideText = overrideText == null ? Optional.empty() : overrideText;
        overrideSignature = overrideSignature == null ? Optional.empty() : overrideSignature;
    }

    static RecordResult passed(SqlTestRecord record) {
        return new RecordResult(record, RecordOutcome.PASSED, "");
    }

    static RecordResult failed(SqlTestRecord record, String failureReason) {
        return new RecordResult(record, RecordOutcome.FAILED, failureReason);
    }

    static RecordResult skipped(SqlTestRecord record, String reason) {
        return new RecordResult(record, RecordOutcome.SKIPPED, reason);
    }

    static RecordResult overridden(SqlTestRecord record, String overrideText) {
        return new RecordResult(record, RecordOutcome.OVERRIDDEN, "", Optional.of(overrideText));
    }

    static RecordResult overridden(SqlTestRecord record, String overrideText, String overrideSignature) {
        return new RecordResult(record, RecordOutcome.OVERRIDDEN, "", Optional.of(overrideText), Optional.of(overrideSignature), false);
    }

    static RecordResult overriddenAsStatementError(SqlTestRecord record, String errorMessage) {
        return new RecordResult(record, RecordOutcome.OVERRIDDEN, "", Optional.of(errorMessage), Optional.empty(), true);
    }

    /** Source location of {@link #record()}, for report lines. */
    public SourceLocation location() {
        return record.location();
    }
}
