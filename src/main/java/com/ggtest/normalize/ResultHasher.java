package com.ggtest.normalize;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MD5 hashing of normalized result values, compatible with the original C
 * sqllogictest implementation (each value followed by {@code '\n'}).
 */
public final class ResultHasher {

    private static final Pattern HASH_LINE = Pattern.compile(
            "^(\\d+) values hashing to ([0-9a-f]{32})$");

    private ResultHasher() {}

    /**
     * Parsed form of an expected {@code N values hashing to <md5>} line.
     *
     * @param valueCount number of values claimed by the expectation
     * @param md5Hex     lowercase hex MD5 digest
     */
    public record HashExpectation(int valueCount, String md5Hex) {}

    /**
     * Computes the lowercase hex MD5 of concatenating each value plus a newline,
     * matching the C {@code md5_add(value); md5_add("\\n");} loop.
     */
    public static String md5Hex(List<String> normalizedValues) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (String value : normalizedValues) {
                digest.update(value.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) '\n');
            }
            return toLowerHex(digest.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("MD5 not available", ex);
        }
    }

    /** Formats {@code N values hashing to <md5>} for {@code normalizedValues}. */
    public static String hashForm(List<String> normalizedValues) {
        return normalizedValues.size()
                + " values hashing to "
                + md5Hex(normalizedValues);
    }

    /**
     * Recognizes a single-line hash expectation.
     *
     * @param expectedText trimmed expected body (may be multi-line; only a single
     *                     hash line is accepted)
     */
    public static Optional<HashExpectation> parseHashExpectation(String expectedText) {
        String trimmed = expectedText == null ? "" : expectedText.strip();
        if (trimmed.isEmpty() || trimmed.indexOf('\n') >= 0) {
            return Optional.empty();
        }
        Matcher matcher = HASH_LINE.matcher(trimmed);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new HashExpectation(
                Integer.parseInt(matcher.group(1)),
                matcher.group(2)));
    }

    private static String toLowerHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }
}
