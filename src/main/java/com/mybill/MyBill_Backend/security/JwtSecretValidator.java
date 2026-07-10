package com.mybill.MyBill_Backend.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Validation wrapper for checking the security and strength of the JWT secret key.
 * This class mitigates known brute-force and dictionary attack CVEs on JWTs.
 */
public class JwtSecretValidator {

    private static final Set<String> WEAK_SECRETS = new HashSet<>(Arrays.asList(
            "secret", "password", "mybill", "mybillsecret", "changeit", "development", "jwtsecret",
            "12345678901234567890123456789012", "abcdefghijklmnopqrstuvwxyzabcdef",
            "qwertyuiopasdfghjklzxcvbnmqwerty"
    ));

    /**
     * Validates a secret key against length, entropy, placeholders, and weak keys dictionary.
     * @param secret the raw secret key configured.
     * @throws IllegalStateException if configuration placeholders are unresolved or missing.
     * @throws IllegalArgumentException if the secret key is too short or is considered cryptographically weak.
     */
    public static void validate(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("FATAL CONFIG ERROR: 'jwt.secret' environment injection property is missing or empty.");
        }

        if (secret.contains("${")) {
            throw new IllegalStateException("FATAL CONFIG ERROR: 'jwt.secret' environment injection property is an unresolved configuration placeholder.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("FATAL CONFIG ERROR: 'jwt.secret' payload length is insufficient. It must be at least 32 bytes (256-bit) long to mitigate brute-force signature forgery.");
        }

        String normalizedSecret = secret.trim().toLowerCase();
        if (WEAK_SECRETS.contains(normalizedSecret)) {
            throw new IllegalArgumentException("FATAL CONFIG ERROR: 'jwt.secret' matches a common weak dictionary key. Do not use common words, simple strings, or repeated patterns.");
        }

        // Validate character diversity (entropy check)
        long uniqueChars = secret.chars().distinct().count();
        if (uniqueChars < 8) {
            throw new IllegalArgumentException("FATAL CONFIG ERROR: 'jwt.secret' has extremely low entropy. It must contain at least 8 unique characters.");
        }
    }
}
