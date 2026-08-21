package com.ggtest.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Parses {@code ggtest} command-line arguments into {@link ParsedArguments}.
 *
 * <p>Does not open connections, read {@code .env}, or read the process
 * environment. Usage errors raise {@link UsageException}; callers map those to
 * exit code 2. Merging and validation happen in {@link RuntimeConfigResolver}.
 *
 * <p>Color: {@code --color <auto|always|never>} (default resolved later as
 * {@code auto}). Priority over system property {@code ggtest.color} and env
 * {@code GGTEST_COLOR} is applied in {@link RuntimeConfigResolver}.
 *
 * <p>Halt: {@code --halt} (no value; default off). Repeating it is equivalent
 * to supplying it once. Short forms or prefixes ({@code -halt}, {@code --hal})
 * are rejected as unknown options.
 */
public final class CliArgumentParser {

    public static final String DEFAULT_ENGINE = "sqlite";

    /**
     * The complete set of recognized option flags. A value token is treated as
     * a missing value only when the next argv element is one of these flags;
     * any other token (including one that starts with {@code -}, such as a
     * password {@code -secret} or a negative number) is accepted as the value.
     */
    private static final Set<String> OPTION_FLAGS = Set.of(
            "--url", "--user", "--password", "--engine", "--hash-threshold",
            "--env-file", "--color", "--parallel", "--halt", "--override",
            "--separator", "--trace", "--help", "-h");

    private CliArgumentParser() {}

    /**
     * @param args raw argv (excluding the program name)
     * @return argv-only parse result (URL may be absent)
     * @throws UsageException when options are malformed
     */
    public static ParsedArguments parse(String[] args) {
        if (args == null) {
            throw new UsageException("missing arguments");
        }

        Optional<String> url = Optional.empty();
        Optional<String> user = Optional.empty();
        Optional<String> password = Optional.empty();
        Optional<String> engine = Optional.empty();
        Optional<Integer> hashThreshold = Optional.empty();
        Optional<String> envFile = Optional.empty();
        Optional<ColorMode> color = Optional.empty();
        boolean halt = false;
        boolean override = false;
        boolean trace = false;
        boolean help = false;
        Optional<Integer> parallel = Optional.empty();
        Optional<String> overrideSeparator = Optional.empty();
        List<String> inputs = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                switch (arg) {
                    case "--url" -> url = Optional.of(requireValue(args, ++i, "--url"));
                    case "--user" -> user = Optional.of(requireValue(args, ++i, "--user"));
                    case "--password" -> password = Optional.of(requireValue(args, ++i, "--password"));
                    case "--engine" -> engine = Optional.of(requireValue(args, ++i, "--engine"));
                    case "--hash-threshold" ->
                        hashThreshold = Optional.of(parseHashThreshold(requireValue(args, ++i, "--hash-threshold")));
                    case "--env-file" -> envFile = Optional.of(requireValue(args, ++i, "--env-file"));
                    case "--color" -> color = Optional.of(ColorMode.parse(requireValue(args, ++i, "--color"), "--color"));
                    case "--parallel" -> {
                        String val = requireValue(args, ++i, "--parallel");
                        int n;
                        try {
                            n = Integer.parseInt(val);
                        } catch (NumberFormatException e) {
                            throw new UsageException("invalid --parallel value: " + val);
                        }
                        if (n < 1) {
                            throw new UsageException("--parallel must be >= 1, got: " + n);
                        }
                        parallel = Optional.of(n);
                    }
                    case "--halt" -> halt = true;
                    case "--override" -> override = true;
                    case "--separator" -> {
                        String sep = requireValue(args, ++i, "--separator");
                        if (sep.isEmpty()) {
                            throw new UsageException("--separator value must not be empty");
                        }
                        if (sep.chars().anyMatch(Character::isWhitespace)) {
                            throw new UsageException("--separator value must not contain whitespace");
                        }
                        overrideSeparator = Optional.of(sep);
                    }
                    case "--trace" -> trace = true;
                    case "--help", "-h" -> help = true;
                    default -> throw new UsageException("unknown option: " + arg);
                }
            } else {
                inputs.add(arg);
            }
        }

        if (parallel.isPresent() && override) {
            throw new UsageException("--parallel and --override cannot be used together");
        }
        if (overrideSeparator.isPresent() && !override) {
            throw new UsageException("--separator requires --override");
        }

        return new ParsedArguments(url, user, password, engine, hashThreshold, envFile, color, halt, override, trace, help,
                parallel, overrideSeparator, inputs);
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new UsageException("missing value for " + option);
        }
        String next = args[index];
        if (OPTION_FLAGS.contains(next)) {
            throw new UsageException("missing value for " + option);
        }
        return next;
    }

    private static int parseHashThreshold(String raw) {
        try {
            int value = Integer.parseInt(raw);
            if (value < 0) {
                throw new UsageException("--hash-threshold must be non-negative, got: " + value);
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new UsageException("invalid --hash-threshold value: " + raw);
        }
    }
}
