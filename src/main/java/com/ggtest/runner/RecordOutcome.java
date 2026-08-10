package com.ggtest.runner;

/**
 * Verdict the runner assigns to an assertable record ({@code statement} or
 * {@code query}). Directive records carry no verdict.
 */
public enum RecordOutcome {

    /** The record's assertion held. */
    PASSED,

    /** The record ran (or could not be compared) and its assertion did not hold. */
    FAILED,

    /** The record was not executed: a {@code skipif}/{@code onlyif} condition or an earlier {@code halt}. */
    SKIPPED,

    /**
     * The record had an in-scope mismatch whose expected interval was overridden by the
     * actual output (golden-update mode). Not a failure: does not count toward failed
     * counts and does not trigger {@code --halt}.
     */
    OVERRIDDEN
}
