package com.ggtest.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Minimal {@code KEY=VALUE} loader for CLI configuration.
 *
 * <p>Supports {@code #} comments, blank lines, and a single pair of matching
 * quotes around values. Unknown or malformed lines are ignored. Does not
 * support {@code export}, interpolation, or multi-line values.
 */
public final class DotEnvLoader {

    static final Set<String> WHITELIST = Set.of(
            "GGTEST_URL",
            "GGTEST_USER",
            "GGTEST_PASSWORD",
            "GGTEST_ENGINE",
            "GGTEST_HASH_THRESHOLD");

    private DotEnvLoader() {}

    /**
     * Loads whitelist keys from {@code path}.
     *
     * @throws UsageException when the file cannot be read
     */
    public static Map<String, String> load(Path path) {
        Objects.requireNonNull(path, "path");
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            Map<String, String> values = new LinkedHashMap<>();
            for (String raw : lines) {
                parseLine(raw).ifPresent(entry -> {
                    if (WHITELIST.contains(entry.key())) {
                        values.put(entry.key(), entry.value());
                    }
                });
            }
            return Collections.unmodifiableMap(values);
        } catch (IOException ex) {
            throw new UsageException("cannot read env file: " + path);
        }
    }

    static Optional<Entry> parseLine(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String line = raw.strip();
        if (line.isEmpty() || line.startsWith("#")) {
            return Optional.empty();
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
            return Optional.empty();
        }
        String key = line.substring(0, eq).strip();
        if (key.isEmpty()) {
            return Optional.empty();
        }
        String value = stripQuotes(line.substring(eq + 1).strip());
        return Optional.of(new Entry(key, value));
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    record Entry(String key, String value) {}
}
