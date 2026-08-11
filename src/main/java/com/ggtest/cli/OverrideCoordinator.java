package com.ggtest.cli;

import com.ggtest.runner.FileRunResult;
import com.ggtest.runner.RecordOutcome;
import com.ggtest.runner.RecordResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Collects override candidates from a run result and writes them back to the source file. */
final class OverrideCoordinator {

    private final ReportWriter reportWriter;
    private final Function<String, String> sanitize;

    OverrideCoordinator(ReportWriter reportWriter, Function<String, String> sanitize) {
        this.reportWriter = Objects.requireNonNull(reportWriter, "reportWriter");
        this.sanitize = Objects.requireNonNull(sanitize, "sanitize");
    }

    List<OverrideWriter.Override> collectOverrides(FileRunResult result) {
        List<OverrideWriter.Override> overrides = new ArrayList<>();
        for (RecordResult rr : result.recordResults()) {
            if (rr.outcome() == RecordOutcome.OVERRIDDEN && rr.overrideText().isPresent()) {
                overrides.add(new OverrideWriter.Override(rr.record(), rr.overrideText().orElseThrow()));
            }
        }
        return overrides;
    }

    FileOutcome applyOverrideWriteBack(
            Path file, List<OverrideWriter.Override> overrides, String display) {
        OverrideWriter overrideWriter = new OverrideWriter();
        try {
            String original = Files.readString(file, StandardCharsets.UTF_8);
            String rewritten = overrideWriter.rewrite(original, overrides);
            overrideWriter.writeAtomically(file, rewritten);
        } catch (IOException ex) {
            List<String> hardDetail = new ArrayList<>(reportWriter.detailLines(
                    "override write failed: " + sanitize.apply(ex.getMessage()),
                    null,
                    display,
                    null));
            for (OverrideWriter.Override ov : overrides) {
                hardDetail.addAll(reportWriter.detailLines(
                        "would have overridden record at line " + ov.record().location().startLine(),
                        null,
                        display,
                        ov.record().location().startLine()));
            }
            return FileOutcome.hardFailure(hardDetail);
        }
        return null;
    }
}
