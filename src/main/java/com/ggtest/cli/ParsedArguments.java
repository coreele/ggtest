package com.ggtest.cli;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Argv-only parse result. Does not read files or process environment.
 * Field absence means the flag was not provided on the command line.
 */
public record ParsedArguments(
        Optional<String> url,
        Optional<String> user,
        Optional<String> password,
        Optional<String> engine,
        Optional<Integer> hashThreshold,
        Optional<String> envFile,
        List<String> inputs) {

    public ParsedArguments {
        url = url == null ? Optional.empty() : url;
        user = user == null ? Optional.empty() : user;
        password = password == null ? Optional.empty() : password;
        engine = engine == null ? Optional.empty() : engine;
        hashThreshold = hashThreshold == null ? Optional.empty() : hashThreshold;
        envFile = envFile == null ? Optional.empty() : envFile;
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
    }
}
