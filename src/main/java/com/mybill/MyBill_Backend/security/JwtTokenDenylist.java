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
