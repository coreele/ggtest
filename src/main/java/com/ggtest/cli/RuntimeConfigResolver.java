package com.ggtest.cli;

import com.ggtest.normalize.ResultComparer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Merges CLI argv, process environment, and optional {@code .env} into
 * {@link CliOptions}, then validates engine allow-list and engine↔URL pairing.
 *
 * <p>Field-level priority for connection fields: CLI &gt; process environment &gt;
 * {@code .env}.
 *
 * <p>Color priority (Q-R4): explicit CLI {@code --color} &gt; system property
 * {@value #COLOR_PROPERTY} &gt; env {@value #COLOR_ENV} &gt; default {@code auto}.
 */
public final class RuntimeConfigResolver {

    public static final String ENGINE_SQLITE = "sqlite";
    public static final String ENGINE_POSTGRES = "postgres";
    public static final String ENGINE_MYSQL = "mysql";
    public static final String ENGINE_XUGU = "xugu";
    /** System property key for report color ({@code -Dggtest.color=…}). */
    public static final String COLOR_PROPERTY = "ggtest.color";

    /** Process environment key for report color. */
    public static final String COLOR_ENV = "GGTEST_COLOR";

    private RuntimeConfigResolver() {}

    /**
     * Resolves final options from parsed argv using {@link System#getProperty(String)}.
     */
    public static CliOptions resolve(
            ParsedArguments parsed,
            Function<String, String> envLookup,
            Path workingDirectory) {
        return resolve(parsed, envLookup, workingDirectory, System::getProperty);
    }

    /**
     * Resolves final options from parsed argv.
     *
     * @param parsed argv-only parse result
     * @param envLookup process environment reader (whitelist keys + {@link #COLOR_ENV})
     * @param workingDirectory directory used for the default {@code .env} path
     * @param propertyLookup system property reader (uses {@link #COLOR_PROPERTY})
     */
    public static CliOptions resolve(
            ParsedArguments parsed,
            Function<String, String> envLookup,
            Path workingDirectory,
            Function<String, String> propertyLookup) {
        Objects.requireNonNull(parsed, "parsed");
        Objects.requireNonNull(envLookup, "envLookup");
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        Objects.requireNonNull(propertyLookup, "propertyLookup");

        Map<String, String> fileValues = loadDotEnv(parsed, workingDirectory);
        Map<String, String> envValues = readProcessEnv(envLookup);

        String url = firstPresent(
                parsed.url().filter(v -> !v.isBlank()),
                optionalEnv(envValues, "GGTEST_URL"),
                optionalEnv(fileValues, "GGTEST_URL"));
        if (url == null || url.isBlank()) {
            throw new UsageException("missing required option: --url (or GGTEST_URL)");
        }

        Optional<String> user = firstOptional(
                parsed.user(),
                optionalEnv(envValues, "GGTEST_USER"),
                optionalEnv(fileValues, "GGTEST_USER"));
        Optional<String> password = firstOptional(
                parsed.password(),
                optionalEnv(envValues, "GGTEST_PASSWORD"),
                optionalEnv(fileValues, "GGTEST_PASSWORD"));

        String engineRaw = firstPresent(
                parsed.engine(),
                optionalEnv(envValues, "GGTEST_ENGINE"),
                optionalEnv(fileValues, "GGTEST_ENGINE"));
        String engine = normalizeEngine(engineRaw == null ? CliArgumentParser.DEFAULT_ENGINE : engineRaw);

        int hashThreshold = resolveHashThreshold(parsed, envValues, fileValues);
        ColorMode colorMode = resolveColorMode(parsed, envLookup, propertyLookup);

        if (parsed.inputs().isEmpty()) {
            throw new UsageException("at least one file or directory path is required");
        }

        validateEngineUrlPair(engine, url);

        int parallelVal = parsed.parallel().orElse(0);

        return new CliOptions(
                url, user, password, engine, hashThreshold, colorMode, parsed.halt(), parsed.override(), parsed.trace(),
                parallelVal, parsed.inputs(), parsed.overrideSeparator());
    }

    static ColorMode resolveColorMode(
            ParsedArguments parsed,
            Function<String, String> envLookup,
            Function<String, String> propertyLookup) {
        if (parsed.color().isPresent()) {
            return parsed.color().orElseThrow();
        }
        String fromProperty = propertyLookup.apply(COLOR_PROPERTY);
        if (fromProperty != null && !fromProperty.isBlank()) {
            return ColorMode.parse(fromProperty, "-D" + COLOR_PROPERTY);
        }
        String fromEnv = envLookup.apply(COLOR_ENV);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return ColorMode.parse(fromEnv, COLOR_ENV);
        }
        return ColorMode.AUTO;
    }

    static boolean resolveAnsiEnabled(ColorMode mode, boolean tty) {
        return switch (mode) {
            case ALWAYS -> true;
            case NEVER -> false;
            case AUTO -> tty;
        };
    }

    private static Map<String, String> loadDotEnv(ParsedArguments parsed, Path workingDirectory) {
        if (parsed.envFile().isPresent()) {
            Path explicit = Path.of(parsed.envFile().orElseThrow());
            if (!Files.isRegularFile(explicit) || !Files.isReadable(explicit)) {
                throw new UsageException("cannot read env file: " + explicit);
            }
            return DotEnvLoader.load(explicit);
        }
        Path defaultPath = workingDirectory.toAbsolutePath().normalize().resolve(".env");
        if (!Files.isRegularFile(defaultPath)) {
            return Map.of();
        }
        return DotEnvLoader.load(defaultPath);
    }

    private static Map<String, String> readProcessEnv(Function<String, String> envLookup) {
        java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        for (String key : DotEnvLoader.WHITELIST) {
            String value = envLookup.apply(key);
            if (value != null) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static int resolveHashThreshold(
            ParsedArguments parsed, Map<String, String> envValues, Map<String, String> fileValues) {
        if (parsed.hashThreshold().isPresent()) {
            return parsed.hashThreshold().orElseThrow();
        }
        String fromEnv = envValues.get("GGTEST_HASH_THRESHOLD");
        if (fromEnv != null) {
            return parseHashThreshold(fromEnv);
        }
        String fromFile = fileValues.get("GGTEST_HASH_THRESHOLD");
        if (fromFile != null) {
            return parseHashThreshold(fromFile);
        }
        return ResultComparer.DEFAULT_HASH_THRESHOLD;
    }

    private static int parseHashThreshold(String raw) {
        try {
            return Integer.parseInt(raw.strip());
        } catch (NumberFormatException ex) {
            throw new UsageException("invalid hash-threshold value: " + raw);
        }
    }

    static String normalizeEngine(String engine) {
        String normalized = engine.strip().toLowerCase(Locale.ROOT);
        // "xugudb" is an accepted alias, normalized to the canonical "xugu".
        if ("xugudb".equals(normalized)) {
            normalized = ENGINE_XUGU;
        }
        if (!ENGINE_SQLITE.equals(normalized) && !ENGINE_POSTGRES.equals(normalized)
                && !ENGINE_XUGU.equals(normalized) && !ENGINE_MYSQL.equals(normalized)) {
            throw new UsageException(
                    "unsupported --engine value '" + engine + "'; allowed: 'sqlite', 'postgres', 'xugu', 'mysql'");
        }
        return normalized;
    }

    static void validateEngineUrlPair(String engine, String url) {
        String lowerUrl = url.toLowerCase(Locale.ROOT);
        if (ENGINE_SQLITE.equals(engine)) {
            if (!lowerUrl.startsWith("jdbc:sqlite:")) {
                throw new UsageException("engine 'sqlite' requires a jdbc:sqlite: URL");
            }
            return;
        }
        if (ENGINE_POSTGRES.equals(engine)) {
            if (!lowerUrl.startsWith("jdbc:postgresql:")) {
                throw new UsageException("engine 'postgres' requires a jdbc:postgresql: URL");
            }
            return;
        }
        if (ENGINE_MYSQL.equals(engine)) {
            if (!lowerUrl.startsWith("jdbc:mysql:")) {
                throw new UsageException("engine 'mysql' requires a jdbc:mysql: URL");
            }
            return;
        }
        if (ENGINE_XUGU.equals(engine)) {
            if (!lowerUrl.startsWith("jdbc:xugu:")) {
                throw new UsageException("engine 'xugu' requires a jdbc:xugu: URL");
            }
        }
    }

    private static Optional<String> optionalEnv(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if ("GGTEST_URL".equals(key) && value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    @SafeVarargs
    private static String firstPresent(Optional<String>... layers) {
        for (Optional<String> layer : layers) {
            if (layer != null && layer.isPresent()) {
                String value = layer.get();
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    @SafeVarargs
    private static Optional<String> firstOptional(Optional<String>... layers) {
        for (Optional<String> layer : layers) {
            if (layer != null && layer.isPresent()) {
                return layer;
            }
        }
        return Optional.empty();
    }
}
