package com.ggtest.runner;

import java.util.List;
import java.util.Objects;

/**
 * Result of running one test file: the per-record verdicts in file order plus
 * the file-level summary a CLI needs for statistics and exit codes.
 *
 * <p>Only assertable records ({@code statement}, {@code query}) appear in
 * {@code recordResults}; directives shape the run instead of being asserted.
 *
 * @param recordResults per-record verdicts, in file order
 * @param halted        whether an executed {@code halt} stopped the file
 * @param aborted       whether a fatal database failure stopped the file
 * @param abortReason   fatal failure description; empty unless {@code aborted}
 */
public record FileRunResult(
        List<RecordResult> recordResults,
        boolean halted,
        boolean aborted,
        String abortReason) {

    public FileRunResult {
        recordResults = List.copyOf(Objects.requireNonNull(recordResults, "recordResults"));
        abortReason = abortReason == null ? "" : abortReason;
    }

    /** Number of records whose assertion held. */
    public int passedCount() {
        return count(RecordOutcome.PASSED);
    }

    /** Number of records whose assertion did not hold. */
    public int failedCount() {
        return count(RecordOutcome.FAILED);
    }

    /** Number of records not executed because of a condition or an earlier {@code halt}. */
    public int skippedCount() {
        return count(RecordOutcome.SKIPPED);
    }

    /** Number of records whose in-scope mismatch was overridden (golden-update mode). */
    public int overriddenCount() {
        return count(RecordOutcome.OVERRIDDEN);
    }

    private int count(RecordOutcome outcome) {
        int total = 0;
        for (RecordResult result : recordResults) {
            if (result.outcome() == outcome) {
                total++;
            }
        }
        return total;
    }
}
