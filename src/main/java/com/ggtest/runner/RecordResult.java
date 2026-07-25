package com.ggtest.runner;

import com.ggtest.model.SourceLocation;
import com.ggtest.model.SqlTestRecord;
import java.util.Objects;

/**
 * Verdict for a single assertable record, plus the material a report needs to
 * explain a failure or a skip.
 *
 * @param record        the executed (or skipped) record
 * @param outcome       the verdict
 * @param failureReason failure or skip explanation; empty for {@link RecordOutcome#PASSED}
 */
public record RecordResult(SqlTestRecord record, RecordOutcome outcome, String failureReason) {

    public RecordResult {
        Objects.requireNonNull(record, "record");
        Objects.requireNonNull(outcome, "outcome");
        failureReason = failureReason == null ? "" : failureReason;
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

    /** Source location of {@link #record()}, for report lines. */
    public SourceLocation location() {
        return record.location();
    }
}
