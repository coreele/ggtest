package com.ggtest.cli;

import java.nio.file.Path;
import java.util.Objects;

/** Source file plus the executable sqllogictest view consumed by the parser. */
record ExecutableDocument(Path path, String sourceName, String content) {

    ExecutableDocument {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(sourceName, "sourceName");
        Objects.requireNonNull(content, "content");
    }
}
