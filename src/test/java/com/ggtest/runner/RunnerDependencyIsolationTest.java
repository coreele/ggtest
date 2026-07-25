package com.ggtest.runner;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * P0-8: the runner reaches databases only through the {@code com.ggtest.db}
 * abstraction, so the SQLite implementation can be replaced wholesale without
 * touching parser or runner sources.
 *
 * <p>Checks production sources only — tests legitimately wire the concrete SQLite
 * executor to exercise the real driver.
 */
class RunnerDependencyIsolationTest {

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java", "com", "ggtest");

    @Test
    void runnerSourcesDoNotDependOnAnyConcreteDatabase() {
        List<String> violations = new ArrayList<>();
        for (Path source : javaSources(MAIN_SOURCES.resolve("runner"))) {
            String body = read(source);
            for (String forbidden : List.of(
                    "com.ggtest.db.sqlite",
                    "com.ggtest.db.postgres",
                    "java.sql",
                    "org.sqlite",
                    "org.xerial",
                    "org.postgresql")) {
                if (body.contains(forbidden)) {
                    violations.add(source + " references " + forbidden);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "runner must depend on com.ggtest.db only: " + violations);
    }

    @Test
    void executorAbstractionStaysFreeOfJdbc() {
        List<String> violations = new ArrayList<>();
        for (Path source : javaSources(MAIN_SOURCES.resolve("db"))) {
            Path parent = source.getParent();
            if (parent.endsWith("sqlite") || parent.endsWith("postgres")) {
                continue;
            }
            String body = read(source);
            for (String forbidden : List.of("java.sql", "org.sqlite", "org.xerial", "org.postgresql")) {
                if (body.contains(forbidden)) {
                    violations.add(source + " references " + forbidden);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "com.ggtest.db must stay driver-agnostic: " + violations);
    }

    @Test
    void parserSourcesStayIndependentOfDatabaseAccess() {
        List<String> violations = new ArrayList<>();
        for (Path source : javaSources(MAIN_SOURCES.resolve("parser"))) {
            String body = read(source);
            for (String forbidden : List.of("com.ggtest.db", "java.sql")) {
                if (body.contains(forbidden)) {
                    violations.add(source + " references " + forbidden);
                }
            }
        }

        assertTrue(violations.isEmpty(), () -> "parser must not reach the database: " + violations);
    }

    private static List<Path> javaSources(Path directory) {
        if (!Files.isDirectory(directory)) {
            return fail("expected source directory " + directory.toAbsolutePath());
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> sources = paths.filter(path -> path.toString().endsWith(".java")).toList();
            if (sources.isEmpty()) {
                fail("no sources found under " + directory.toAbsolutePath());
            }
            return sources;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String read(Path source) {
        try {
            return Files.readString(source);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
