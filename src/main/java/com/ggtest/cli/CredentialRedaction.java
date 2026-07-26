package com.ggtest.cli;

import java.util.Optional;
import java.util.regex.Pattern;

/** Package-private helpers for redacting credentials from diagnostic strings. */
final class CredentialRedaction {

    private static final Pattern URL_USERINFO = Pattern.compile("([^/]*://)([^/@]+)@");

    private CredentialRedaction() {}

    static String redactUrlUserInfo(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return URL_USERINFO.matcher(text).replaceAll("$1***@");
    }

    static String redactMessage(String message, Optional<String> configuredPassword) {
        String result = message == null ? "" : message.strip();
        result = redactUrlUserInfo(result);
        if (configuredPassword != null && configuredPassword.isPresent()) {
            String password = configuredPassword.get();
            if (!password.isEmpty()) {
                result = result.replace(password, "***");
            }
        }
        return result;
    }
}
