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
 * @param colorMode     report color mode ({@code auto}/{@code always}/{@code never})
 * @param halt          whether {@code --halt} (stop on first error) is enabled;
 *                      CLI-only; {@code false} when absent
 * @param override      whether {@code --override} (golden-update) is enabled;
 *                      CLI-only; {@code false} when absent
 * @param inputs        positional file or directory paths (at least one)
 */
public record CliOptions(
        String url,
        Optional<String> user,
        Optional<String> password,
        String engine,
        int hashThreshold,
        ColorMode colorMode,
        boolean halt,
        boolean override,
        List<String> inputs) {

    public CliOptions(
            String url,
            Optional<String> user,
            Optional<String> password,
            String engine,
            int hashThreshold,
            ColorMode colorMode,
            boolean halt,
            List<String> inputs) {
        this(url, user, password, engine, hashThreshold, colorMode, halt, false, inputs);
    }

    public CliOptions {
        Objects.requireNonNull(url, "url");
        user = user == null ? Optional.empty() : user;
        password = password == null ? Optional.empty() : password;
        Objects.requireNonNull(engine, "engine");
        Objects.requireNonNull(colorMode, "colorMode");
        inputs = List.copyOf(Objects.requireNonNull(inputs, "inputs"));
    }

    /** Omits password plaintext from diagnostic dumps. */
    @Override
    public String toString() {
        return "CliOptions[url="
                + CredentialRedaction.redactUrlUserInfo(url)
                + ", user="
                + user
                + ", password="
                + (password.isPresent() ? Optional.of("***") : Optional.empty())
                + ", engine="
                + engine
                + ", hashThreshold="
                + hashThreshold
                + ", colorMode="
                + colorMode
                + ", halt="
                + halt
                + ", override="
                + override
                + ", inputs="
                + inputs
                + "]";
    }
}
