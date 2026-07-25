package com.ggtest.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Resolves CLI positional paths into the ordered list of test files to run.
 *
 * <p>Directories are walked recursively for {@code *.test} and {@code *.slt}.
 * Explicit file paths are accepted regardless of extension. Results are absolute,
 * normalized, de-duplicated, and sorted by absolute path string order.
 */
public final class TestFileCollector {

    private TestFileCollector() {}

    /**
     * @param inputs positional file or directory paths from the CLI
     * @return absolute, normalized, stably sorted test file paths
     * @throws UsageException when a path is missing or a directory yields no matches
     */
    public static List<Path> collect(List<String> inputs) {
        Objects.requireNonNull(inputs, "inputs");
        LinkedHashSet<Path> discovered = new LinkedHashSet<>();
        for (String input : inputs) {
            Path path = Path.of(input).toAbsolutePath().normalize();
            if (!Files.exists(path)) {
                throw new UsageException("path not found: " + path);
            }
            if (Files.isDirectory(path)) {
                discovered.addAll(collectFromDirectory(path));
            } else if (Files.isRegularFile(path)) {
                discovered.add(path);
            } else {
                throw new UsageException("not a file or directory: " + path);
            }
        }
        if (discovered.isEmpty()) {
            throw new UsageException("no *.test or *.slt files found under the given paths");
        }
        List<Path> ordered = new ArrayList<>(discovered);
        ordered.sort(Comparator.comparing(path -> path.toString()));
        return List.copyOf(ordered);
    }

    private static List<Path> collectFromDirectory(Path directory) {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(TestFileCollector::hasCorpusExtension)
                    .map(path -> path.toAbsolutePath().normalize())
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException ex) {
            throw new UsageException("failed to read directory: " + directory + ": " + ex.getMessage());
        }
    }

    private static boolean hasCorpusExtension(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".test") || name.endsWith(".slt");
    }
}
