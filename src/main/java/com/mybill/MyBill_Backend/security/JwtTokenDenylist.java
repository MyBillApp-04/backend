package com.mybill.MyBill_Backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

/**
 * In-memory JWT token denylist using Caffeine cache.
 *
 * <h3>Design & Architecture Considerations</h3>
 * <p>Tokens are denied when a user calls {@code /api/auth/logout}. The tokens are hashed using SHA-256
 * and stored with a 24-hour expiration (matching the default JWT lifetime).
 *
 * <p><strong>Single-Instance Constraints (Render Free Tier):</strong>
 * Because this denylist is stored in the JVM heap, it is ephemeral:
 * <ul>
 *   <li>It is reset whenever the application restarts or is redeployed.</li>
 *   <li>It is not shared across multiple nodes.</li>
 * </ul>
 * For the current single-instance deployment, this is acceptable, cost-effective, and has zero external dependency overhead.
 *
 * <p><strong>Production Scaling:</strong>
 * If the application is scaled horizontally (multi-instance/clustered) or requires persistent logout states across restarts,
 * this component must be replaced with a shared, persistent store:
 * <ul>
 *   <li><strong>Redis:</strong> Store token hashes as keys with a TTL matching the token's remaining lifetime.
 *       This is the industry standard for distributed token denylists.</li>
 *   <li><strong>Database Store:</strong> Log denied tokens in a relational table, cleaned up periodically by a background scheduled task.</li>
 * </ul>
 */
@Component
public class JwtTokenDenylist {

    private final Cache<String, Boolean> deniedTokens = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(24))
            .maximumSize(100_000)
            .build();

    public void deny(String token, Date expiresAt) {
        if (token == null || token.isBlank()) return;

        Instant expiry = expiresAt == null ? Instant.now().plus(Duration.ofHours(24)) : expiresAt.toInstant();
        if (expiry.isAfter(Instant.now())) {
            deniedTokens.put(hash(token), true);
        }
    }

    public boolean isDenied(String token) {
        if (token == null || token.isBlank()) return false;
        return Boolean.TRUE.equals(deniedTokens.getIfPresent(hash(token)));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }
}
