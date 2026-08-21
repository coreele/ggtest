package com.ggtest.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;

/** Loads CLI input files into the executable text handed to the SLT parser. */
final class ExecutableDocumentLoader {

    private ExecutableDocumentLoader() {}

    static ExecutableDocument load(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        String content = Files.readString(file, StandardCharsets.UTF_8);
        String executable = isMarkdown(file)
                ? MarkdownExecutableExtractor.toExecutableSlt(content)
                : content;
        return new ExecutableDocument(file, file.toString(), executable);
    }

    private static boolean isMarkdown(Path file) {
        Path name = file.getFileName();
        return name != null && name.toString().toLowerCase(Locale.ROOT).endsWith(".md");
    }
}
