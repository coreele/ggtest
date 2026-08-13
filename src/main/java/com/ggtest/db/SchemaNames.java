package com.ggtest.db;

import java.util.Locale;
import java.util.UUID;

/**
 * Shared generation and validation of per-file isolation schema names.
 *
 * <p>Schema names are generated as {@code ggtest_} + a lowercase hex UUID, so a
 * freshly {@link #generate()}d name is always a safe SQL identifier. Public
 * entry points that receive externally-supplied names ({@code SET search_path},
 * {@code USE}, {@code DROP SCHEMA}) defend themselves with {@link #isSafe} /
 * {@link #requireSafe} rather than interpolating raw text into DDL.
 *
 * <p>Centralized here so the PostgreSQL and MySQL isolation helpers cannot
 * drift apart in either identifier rules or name format. This type deliberately
 * stays free of the JDBC API so the {@code com.ggtest.db} abstraction package
 * remains driver-agnostic; unsafe-name rejection is an unchecked programming
 * error ({@link IllegalArgumentException}).
 */
public final class SchemaNames {

    private static final String PREFIX = "ggtest_";

    private SchemaNames() {}

    /**
     * Generates a fresh unique schema name that is always a safe identifier.
     *
     * @return a name of the form {@code ggtest_<lowercase hex uuid>}
     */
    public static String generate() {
        return PREFIX + UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ROOT);
    }

    /**
     * A safe SQL identifier for the isolation schema dialects used here:
     * an ASCII lowercase letter followed by one or more ASCII letters, digits
     * or underscores.
     *
     * @param name the name to test (may be {@code null})
     * @return {@code true} iff the name is a safe identifier
     */
    public static boolean isSafe(String name) {
        return name != null && name.matches("[a-z][a-z0-9_]*");
    }

    /**
     * Validates that {@code name} is a safe identifier, returning it on success.
     *
     * @param name the name supplied by a caller
     * @return the validated name
     * @throws IllegalArgumentException if the name is not a safe identifier
     *     (refuses to interpolate unsafe text into DDL such as
     *     {@code SET search_path} / {@code USE} / {@code DROP SCHEMA})
     */
    public static String requireSafe(String name) {
        if (!isSafe(name)) {
            throw new IllegalArgumentException("refusing to use unsafe schema name: " + name);
        }
        return name;
    }
}
