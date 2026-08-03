package com.mybill.MyBill_Backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mybill.MyBill_Backend.entity.RevokedToken;
import com.mybill.MyBill_Backend.repository.RevokedTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;

/**
 * JWT token denylist backed by an in-memory L1 Caffeine cache and PostgreSQL persistent storage.
 *
 * <p>Tokens are denied when a user calls {@code /api/auth/logout}. Token hashes (SHA-256) are saved to
 * PostgreSQL to guarantee revocation persists across application restarts and across multi-instance clusters.
 */
@Component
@Slf4j
public class JwtTokenDenylist {

    private final Cache<String, Boolean> deniedTokens;
    private final RevokedTokenRepository revokedTokenRepository;

    @Autowired
    public JwtTokenDenylist(
            @Value("${app.security.jwt-denylist.cache-max-size:10000}") long cacheMaxSize,
            @Autowired(required = false) RevokedTokenRepository revokedTokenRepository
    ) {
        this.deniedTokens = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofHours(24))
                .maximumSize(cacheMaxSize)
                .build();
        this.revokedTokenRepository = revokedTokenRepository;
    }

    public JwtTokenDenylist(long cacheMaxSize) {
        this(cacheMaxSize, null);
    }

    @Transactional
    public void deny(String token, Date expiresAt) {
        if (token == null || token.isBlank()) return;

        Instant expiry = expiresAt == null ? Instant.now().plus(Duration.ofHours(24)) : expiresAt.toInstant();
        Instant now = Instant.now();
        if (expiry.isAfter(now)) {
            String tokenHash = hash(token);
            deniedTokens.put(tokenHash, true);

            if (revokedTokenRepository != null) {
                try {
                    if (!revokedTokenRepository.existsByTokenHash(tokenHash)) {
                        RevokedToken revokedToken = new RevokedToken(tokenHash, expiry, now);
                        revokedTokenRepository.save(revokedToken);
                    }
                } catch (Exception e) {
                    log.warn("Failed to persist revoked token to database: {}", e.getMessage());
                }
            }
        }
    }

    public boolean isDenied(String token) {
        if (token == null || token.isBlank()) return false;
        String tokenHash = hash(token);

        Boolean cached = deniedTokens.getIfPresent(tokenHash);
        if (cached != null) {
            return cached;
        }

        if (revokedTokenRepository != null) {
            try {
                boolean inDb = revokedTokenRepository.existsByTokenHashAndExpiresAtAfter(tokenHash, Instant.now());
                deniedTokens.put(tokenHash, inDb);
                return inDb;
            } catch (Exception e) {
                log.warn("Failed to check token revocation status in database: {}", e.getMessage());
            }
        }

        return false;
    }

    @Scheduled(cron = "${app.security.jwt-denylist.cleanup-cron:0 0 * * * *}")
    @Transactional
    public int cleanupExpiredTokens() {
        if (revokedTokenRepository == null) return 0;
        try {
            int deleted = revokedTokenRepository.deleteExpiredTokens(Instant.now());
            if (deleted > 0) {
                log.info("Cleaned up {} expired revoked tokens from database", deleted);
            }
            return deleted;
        } catch (Exception e) {
            log.warn("Failed to clean up expired revoked tokens: {}", e.getMessage());
            return 0;
        }
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
