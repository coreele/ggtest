package com.ggtest.cli;

import java.util.List;

/** Result of running a single test file through {@link FileRunner}. */
record FileOutcome(FileBucket bucket, boolean hardError, List<String> detailLines) {

    static FileOutcome passed() {
        return new FileOutcome(FileBucket.PASSED, false, List.of());
    }

    static FileOutcome skipped() {
        return new FileOutcome(FileBucket.SKIPPED, false, List.of());
    }

    static FileOutcome assertionFailure(List<String> detailLines) {
        return new FileOutcome(FileBucket.FAILED, false, List.copyOf(detailLines));
    }

    static FileOutcome hardFailure(List<String> detailLines) {
        return new FileOutcome(FileBucket.FAILED, true, List.copyOf(detailLines));
    }
}

enum FileBucket {
    PASSED,
    FAILED,
    SKIPPED
}
