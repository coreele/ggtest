package com.ggtest.runner;

import com.ggtest.db.DatabaseExecutor;
import com.ggtest.db.FatalDatabaseException;
import com.ggtest.db.QueryResult;
import com.ggtest.db.StatementResult;
import com.ggtest.model.HaltRecord;
import com.ggtest.model.HashThresholdRecord;
import com.ggtest.model.OnlyIfRecord;
import com.ggtest.model.QueryRecord;
import com.ggtest.model.SkipIfRecord;
import com.ggtest.model.SqlTestRecord;
import com.ggtest.model.StatementRecord;
import com.ggtest.normalize.ResultComparer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Executes the records of one test file in order against a {@link DatabaseExecutor}.
 *
 * <p>Reaches the database only through {@code com.ggtest.db}, so supporting
 * another engine means adding an executor implementation — this class stays
 * unchanged. Result comparison is delegated to {@link ResultComparer}.
 *
 * <p>Per-file state — hash-threshold, pending {@code skipif}/{@code onlyif}
 * conditions, and label results — lives inside a single {@link #run(List)} call;
 * callers reset it by starting a new run for the next file. Column separator for
 * row-wise expectations is taken from each {@link QueryRecord} when the query header
 * declares {@code separator=<delim>}; otherwise expectations are value-per-line.
 * Instances are
 * stateless and reusable, but not thread-safe beyond whatever the supplied
 * executor and its connection allow (execution is serial on one connection).
 *
 * <p>A failing record does not stop the file: only an executed {@code halt}
 * (remaining records become {@link RecordOutcome#SKIPPED}) or a
 * {@link FatalDatabaseException} stops it. With {@code haltOnFirstFailure}
 * enabled (CLI {@code --halt}), the first non-fatal
 * {@link RecordOutcome#FAILED} record stops the file the same way: later
 * assertable records become {@link RecordOutcome#SKIPPED} and never reach the
 * executor. The fatal-abort path is unaffected.
 */
public final class SqlLogicTestRunner {

    private static final String SKIPPED_BY_CONDITION = "not executed: skipif/onlyif condition";
    private static final String SKIPPED_AFTER_HALT = "not executed: halt earlier in this file";
    private static final String SKIPPED_AFTER_FAILURE_HALT =
            "not executed: --halt stopped after an earlier failure in this file";

    private final DatabaseExecutor executor;
    private final int initialHashThreshold;
    private final boolean haltOnFirstFailure;
    private final boolean overrideEnabled;

    /**
     * Uses {@link ResultComparer#DEFAULT_HASH_THRESHOLD} as the initial threshold.
     *
     * @param executor database executor; supplies the engine name for conditions
     */
    public SqlLogicTestRunner(DatabaseExecutor executor) {
        this(executor, ResultComparer.DEFAULT_HASH_THRESHOLD);
    }

    /**
     * @param executor             database executor; supplies the engine name for conditions
     * @param initialHashThreshold threshold each run starts with, before any
     *                             {@code hash-threshold} record; {@code <= 0} disables hashing
     */
    public SqlLogicTestRunner(DatabaseExecutor executor, int initialHashThreshold) {
        this(executor, initialHashThreshold, false);
    }

    /**
     * @param executor             database executor; supplies the engine name for conditions
     * @param initialHashThreshold threshold each run starts with, before any
     *                             {@code hash-threshold} record; {@code <= 0} disables hashing
     * @param haltOnFirstFailure   when {@code true}, the first non-fatal
     *                             {@link RecordOutcome#FAILED} assertable record stops the file:
     *                             later assertable records become {@link RecordOutcome#SKIPPED}
     *                             and are not executed (CLI {@code --halt})
     */
    public SqlLogicTestRunner(
            DatabaseExecutor executor, int initialHashThreshold, boolean haltOnFirstFailure) {
        this(executor, initialHashThreshold, haltOnFirstFailure, false);
    }

    /**
     * @param executor             database executor; supplies the engine name for conditions
     * @param initialHashThreshold threshold each run starts with, before any
     *                             {@code hash-threshold} record; {@code <= 0} disables hashing
     * @param haltOnFirstFailure   when {@code true}, the first non-fatal
     *                             {@link RecordOutcome#FAILED} assertable record stops the file:
     *                             later assertable records become {@link RecordOutcome#SKIPPED}
     *                             and are not executed (CLI {@code --halt})
     * @param overrideEnabled      when {@code true}, in-scope mismatches (query result mismatch
     *                             without label conflict / execution failure / type signature error;
     *                             {@code statement error} message mismatch) yield
     *                             {@link RecordOutcome#OVERRIDDEN} instead of
     *                             {@link RecordOutcome#FAILED} (CLI {@code --override})
     */
    public SqlLogicTestRunner(
            DatabaseExecutor executor,
            int initialHashThreshold,
            boolean haltOnFirstFailure,
            boolean overrideEnabled) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.initialHashThreshold = initialHashThreshold;
        this.haltOnFirstFailure = haltOnFirstFailure;
        this.overrideEnabled = overrideEnabled;
    }

    /**
     * Runs one file's records in order.
     *
     * @param records ordered records, as produced by the parser
     * @return per-record verdicts and the file-level summary
     */
    public FileRunResult run(List<SqlTestRecord> records) {
        Objects.requireNonNull(records, "records");
        FileState state = new FileState(executor.engineName(), initialHashThreshold);
        List<RecordResult> results = new ArrayList<>();
        boolean halted = false;
        boolean haltedOnFirstFailure = false;
        boolean aborted = false;
        String abortReason = "";

        for (SqlTestRecord record : records) {
            if (halted) {
                if (isAssertable(record)) {
                    results.add(RecordResult.skipped(record, SKIPPED_AFTER_HALT));
                }
                continue;
            }
            if (haltedOnFirstFailure) {
                if (isAssertable(record)) {
                    results.add(RecordResult.skipped(record, SKIPPED_AFTER_FAILURE_HALT));
                }
                continue;
            }
            try {
                RecordResult produced = null;
                if (record instanceof SkipIfRecord skipIf) {
                    state.addSkipIf(skipIf.dbName());
                } else if (record instanceof OnlyIfRecord onlyIf) {
                    state.addOnlyIf(onlyIf.dbName());
                } else if (record instanceof HashThresholdRecord hashThreshold) {
                    if (!state.consumePendingSkip()) {
                        state.setHashThreshold(hashThreshold.threshold());
                    }
                } else if (record instanceof HaltRecord) {
                    if (!state.consumePendingSkip()) {
                        halted = true;
                    }
                } else if (record instanceof StatementRecord statement) {
                    produced = state.consumePendingSkip()
                            ? RecordResult.skipped(statement, SKIPPED_BY_CONDITION)
                            : runStatement(statement);
                    results.add(produced);
                } else if (record instanceof QueryRecord query) {
                    produced = state.consumePendingSkip()
                            ? RecordResult.skipped(query, SKIPPED_BY_CONDITION)
                            : runQuery(query, state);
                    results.add(produced);
                } else {
                    throw new IllegalStateException("unsupported record type: " + record.getClass().getName());
                }
                if (haltOnFirstFailure
                        && produced != null
                        && produced.outcome() == RecordOutcome.FAILED) {
                    haltedOnFirstFailure = true;
                }
            } catch (FatalDatabaseException ex) {
                abortReason = describe("fatal database failure", ex.getMessage());
                results.add(RecordResult.failed(record, abortReason));
                aborted = true;
                break;
            }
        }

        return new FileRunResult(results, halted, aborted, abortReason);
    }

    private RecordResult runStatement(StatementRecord record) {
        StatementResult result = executor.executeStatement(record.sql());
        return switch (record.expectation()) {
            case OK -> result.succeeded()
                    ? RecordResult.passed(record)
                    : RecordResult.failed(
                            record, describe("statement expected to succeed but failed", result.errorSummary()));
            case ERROR -> {
                if (result.succeeded()) {
                    yield RecordResult.failed(record, "statement expected to fail but succeeded");
                }
                String expectedMsg = record.expectedErrorMsg();
                if (expectedMsg != null && !expectedMsg.isEmpty()) {
                    String actual = result.errorSummary() == null ? "" : result.errorSummary();
                    if (!actual.toLowerCase(Locale.ROOT).contains(expectedMsg.toLowerCase(Locale.ROOT))) {
                        if (overrideEnabled) {
                            yield RecordResult.overridden(record, actual);
                        }
                        yield RecordResult.failed(record,
                                "statement error message mismatch\n"
                                        + "-   " + expectedMsg + "\n"
                                        + "+   " + actual);
                    }
                }
                yield RecordResult.passed(record);
            }
        };
    }

    private RecordResult runQuery(QueryRecord record, FileState state) {
        QueryResult result = executor.executeQuery(record.sql());
        if (!result.succeeded()) {
            return RecordResult.failed(record, describe("query execution failed", result.errorSummary()));
        }

        ResultComparer.CompareResult comparison;
        try {
            comparison = ResultComparer.compare(
                    record.typeSignature(),
                    record.sortMode(),
                    state.hashThreshold(),
                    record.columnSeparator(),
                    expectedText(record),
                    result.rows());
        } catch (IllegalArgumentException ex) {
            return RecordResult.failed(
                    record, describe("result does not fit the declared type signature", ex.getMessage()));
        }

        List<String> failures = new ArrayList<>();
        boolean resultMismatch = false;
        if (record.hasExpectedResults() && !comparison.passed()) {
            resultMismatch = true;
            failures.add("result mismatch:\n" + comparison.diffSummary());
        }
        boolean labelConflict = false;
        if (record.label().isPresent()) {
            String label = record.label().get();
            List<String> firstView = state.rememberLabelView(label, comparison.actualView());
            if (firstView != null && !firstView.equals(comparison.actualView())) {
                labelConflict = true;
                failures.add(labelConflict(label, firstView, comparison.actualView()));
            }
        }
        if (failures.isEmpty()) {
            return RecordResult.passed(record);
        }
        if (overrideEnabled && resultMismatch && !labelConflict) {
            String overrideText = formatOverrideText(record, comparison.actualView());
            return RecordResult.overridden(record, overrideText);
        }
        return RecordResult.failed(record, String.join("\n", failures));
    }

    private static String formatOverrideText(QueryRecord record, List<String> actualView) {
        if (record.columnSeparator().isEmpty() || actualView.size() == 1) {
            return String.join("\n", actualView);
        }
        int columns = record.typeSignature().size();
        if (columns <= 0) {
            return String.join("\n", actualView);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actualView.size(); i++) {
            if (i > 0 && i % columns == 0) {
                sb.append('\n');
            } else if (i > 0) {
                sb.append(record.columnSeparator().orElseThrow());
            }
            sb.append(actualView.get(i));
        }
        return sb.toString();
    }

    private static String expectedText(QueryRecord record) {
        return record.hasExpectedResults() ? String.join("\n", record.expectedResults()) : "";
    }

    private static String labelConflict(String label, List<String> firstView, List<String> currentView) {
        return "label conflict: label '" + label + "' first produced "
                + firstView.size() + " line(s) " + firstView
                + " but this query produced " + currentView.size() + " line(s) " + currentView;
    }

    private static String describe(String summary, String detail) {
        return detail == null || detail.isEmpty() ? summary : summary + ": " + detail;
    }

    private static boolean isAssertable(SqlTestRecord record) {
        return record instanceof StatementRecord || record instanceof QueryRecord;
    }

    /** Mutable state scoped to a single file run. */
    private static final class FileState {

        private final String engineName;
        private final List<String> skipIf = new ArrayList<>();
        private final List<String> onlyIf = new ArrayList<>();
        private final Map<String, List<String>> labelViews = new HashMap<>();
        private int hashThreshold;

        FileState(String engineName, int initialHashThreshold) {
            this.engineName = engineName == null ? "" : engineName;
            this.hashThreshold = initialHashThreshold;
        }

        void addSkipIf(String dbName) {
            skipIf.add(dbName);
        }

        void addOnlyIf(String dbName) {
            onlyIf.add(dbName);
        }

        /**
         * Reports whether the pending conditions skip the record about to be
         * handled, then clears them: conditions guard exactly one record.
         */
        boolean consumePendingSkip() {
            boolean skip = false;
            for (String dbName : skipIf) {
                if (matchesEngine(dbName)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                for (String dbName : onlyIf) {
                    if (!matchesEngine(dbName)) {
                        skip = true;
                        break;
                    }
                }
            }
            skipIf.clear();
            onlyIf.clear();
            return skip;
        }

        int hashThreshold() {
            return hashThreshold;
        }

        void setHashThreshold(int hashThreshold) {
            this.hashThreshold = hashThreshold;
        }

        /**
         * Stores {@code view} as the label's result on first sight.
         *
         * @return the previously stored view, or {@code null} for a first occurrence
         */
        List<String> rememberLabelView(String label, List<String> view) {
            return labelViews.putIfAbsent(label, List.copyOf(view));
        }

        private boolean matchesEngine(String dbName) {
            return dbName != null && dbName.toLowerCase(Locale.ROOT).equals(engineName.toLowerCase(Locale.ROOT));
        }
    }
}
