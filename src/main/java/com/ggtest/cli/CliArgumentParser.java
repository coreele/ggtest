package com.ggtest.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Parses {@code ggtest} command-line arguments into {@link CliOptions}.
 *
 * <p>Does not open connections or read files. Usage errors raise
 * {@link UsageException}; callers map those to exit code 2.
 */
public final class CliArgumentParser {

    public static final String DEFAULT_ENGINE = "sqlite";
    public static final int DEFAULT_HASH_THRESHOLD = 8;

    private CliArgumentParser() {}

    /**
     * @param args raw argv (excluding the program name)
     * @return validated options
     * @throws UsageException when required options are missing or values are illegal
     */
    public static CliOptions parse(String[] args) {
        if (args == null) {
            throw new UsageException("missing arguments");
        }

        String url = null;
        Optional<String> user = Optional.empty();
        Optional<String> password = Optional.empty();
        String engine = DEFAULT_ENGINE;
        int hashThreshold = DEFAULT_HASH_THRESHOLD;
        List<String> inputs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                switch (arg) {
                    case "--url" -> url = requireValue(args, ++i, "--url");
                    case "--user" -> user = Optional.of(requireValue(args, ++i, "--user"));
                    case "--password" -> password = Optional.of(requireValue(args, ++i, "--password"));
                    case "--engine" -> engine = requireValue(args, ++i, "--engine");
                    case "--hash-threshold" -> hashThreshold = parseHashThreshold(requireValue(args, ++i, "--hash-threshold"));
                    default -> throw new UsageException("unknown option: " + arg);
                }
            } else {
                inputs.add(arg);
            }
        }

        if (url == null || url.isBlank()) {
            throw new UsageException("missing required option: --url");
        }
        if (inputs.isEmpty()) {
            throw new UsageException("at least one file or directory path is required");
        }
        if (!DEFAULT_ENGINE.equalsIgnoreCase(engine)) {
            throw new UsageException("unsupported --engine value '" + engine + "'; only 'sqlite' is allowed");
        }

        return new CliOptions(
                url,
                user,
                password,
                engine.toLowerCase(Locale.ROOT),
                hashThreshold,
                inputs);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length || args[index].startsWith("-")) {
            throw new UsageException("missing value for " + option);
        }
        return args[index];
    }

    private static int parseHashThreshold(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new UsageException("invalid --hash-threshold value: " + raw);
        }
    }
}
