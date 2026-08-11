package com.ggtest.cli;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Argv-only parse result. Does not read files or process environment.
 * Field absence means the flag was not provided on the command line.
 *
 * @param halt whether {@code --halt} (stop on first error) was supplied;
 *             repeated occurrences are equivalent to a single one
 * @param override whether {@code --override} (golden-update) was supplied;
 *                 repeated occurrences are equivalent to a single one
 * @param help whether {@code --help} (or {@code -h}) was supplied
 * @param parallel maximum concurrent files; empty when {@code --parallel} not supplied
 */
public record ParsedArguments(
        Optional<String> url,
        Optional<String> user,
        Optional<String> password,
        Optional<String> engine,
        Optional<Integer> hashThreshold,
        Optional<String> envFile,
        Optional<ColorMode> color,
        boolean halt,
        boolean override,
        boolean help,
        Optional<Integer> parallel,
        List<String> inputs) {

    public ParsedArguments {
        url = url == null ? Optional.empty() : url;
        user = user == null ? Optional.empty() : user;
        password = password == null ? Optional.empty() : password;
        engine = engine == null ? Optional.empty() : engine;
        hashThreshold = hashThreshold == null ? Optional.empty() : hashThreshold;
        envFile = envFile == null ? Optional.empty() : envFile;
        color = color == null ? Optional.empty() : color;
        parallel = parallel == null ? Optional.empty() : parallel;
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
    }
}
