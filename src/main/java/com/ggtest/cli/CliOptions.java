package com.ggtest.cli;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Parsed CLI options for a {@code ggtest} invocation.
 *
 * @param url           JDBC URL (from CLI, env, or {@code .env})
 * @param user          optional database user
 * @param password      optional database password (never written to reports)
 * @param engine        target engine name for skipif/onlyif (default {@code sqlite})
 * @param hashThreshold initial hash-threshold for each file run (default 8)
 * @param inputs        positional file or directory paths (at least one)
 */
public record CliOptions(
        String url,
        Optional<String> user,
        Optional<String> password,
        String engine,
        int hashThreshold,
        List<String> inputs) {

    public CliOptions {
        Objects.requireNonNull(url, "url");
        user = user == null ? Optional.empty() : user;
        password = password == null ? Optional.empty() : password;
        Objects.requireNonNull(engine, "engine");
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
    }

    /** Omits password plaintext from diagnostic dumps. */
    @Override
    public String toString() {
        return "CliOptions[url="
                + url
                + ", user="
                + user
                + ", password="
                + (password.isPresent() ? Optional.of("***") : Optional.empty())
                + ", engine="
                + engine
                + ", hashThreshold="
                + hashThreshold
                + ", inputs="
                + inputs
                + "]";
    }
}
