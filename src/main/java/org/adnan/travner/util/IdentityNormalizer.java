package org.adnan.travner.util;

import java.util.Locale;

/**
 * Canonical form for identity values stored in MongoDB and accepted by APIs.
 *
 * <p>Usernames and emails are trimmed and lowercased with {@link Locale#ROOT}
 * so that "Alice", " alice " and "ALICE" resolve to one account regardless of
 * the JVM's default locale (Turkish-I is the classic failure case).</p>
 */
public final class IdentityNormalizer {

    private IdentityNormalizer() {
    }

    public static String normalizeUsername(String username) {
        return normalize(username);
    }

    public static String normalizeEmail(String email) {
        return normalize(email);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
