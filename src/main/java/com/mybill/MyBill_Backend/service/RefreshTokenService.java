package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.RefreshToken;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.RefreshTokenRepository;
import com.mybill.MyBill_Backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.security.jwt.refresh-expiration:7776000000}")
    private long refreshExpirationMillis;

    private final ConcurrentHashMap<String, CompletableFuture<TokenPair>> inFlightRefreshes = new ConcurrentHashMap<>();

    @Transactional
    public TokenPair issue(User user) {
        return issue(user, null, null);
    }

    @Transactional
    public TokenPair issue(User user, String deviceId, String deviceName) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshExpirationMillis));
        refreshToken.setDeviceId(deviceId);
        refreshToken.setDeviceName(deviceName);
        refreshTokenRepository.save(refreshToken);
        return new TokenPair(jwtUtil.generateToken(user.getEmail(), user.getRole()), rawToken);
    }

    /**
     * Marks the device as trusted (find-or-create the per-device token row) so a
     * subsequent {@code /firebase-login} from this device skips OTP. No client-usable
     * token is returned here; the row carries a placeholder hash until MPIN is verified.
     */
    @Transactional
    public void trustDevice(User user, String deviceId, String deviceName) {
        RefreshToken token = findOrCreateDeviceToken(user, deviceId, deviceName);
        token.setTrusted(true);
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMillis));
        refreshTokenRepository.save(token);
    }

    /**
     * Reuses the single per-device token row, replaces its placeholder hash with a real
     * refresh token, marks the device trusted, and returns a usable access/refresh pair.
     * This is the terminal step of the MPIN flow that hands the app its real session.
     */
    @Transactional
    public TokenPair issueTrusted(User user, String deviceId, String deviceName) {
        RefreshToken token = findOrCreateDeviceToken(user, deviceId, deviceName);

        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        token.setTokenHash(hash(rawToken));
        token.setTrusted(true);
        token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMillis));
        token.setDeviceId(deviceId);
        token.setDeviceName(deviceName);
        refreshTokenRepository.save(token);

        return new TokenPair(jwtUtil.generateToken(user.getEmail(), user.getRole()), rawToken);
    }

    private RefreshToken findOrCreateDeviceToken(User user, String deviceId, String deviceName) {
        return refreshTokenRepository.findFirstByUserIdAndDeviceIdOrderByCreatedAtDesc(user.getId(), deviceId)
                .orElseGet(() -> {
                    RefreshToken token = new RefreshToken();
                    token.setUser(user);
                    token.setDeviceId(deviceId);
                    token.setDeviceName(deviceName);
                    token.setCreatedAt(Instant.now());
                    token.setExpiresAt(Instant.now().plusMillis(refreshExpirationMillis));
                    byte[] bytes = new byte[48];
                    RANDOM.nextBytes(bytes);
                    token.setTokenHash(hash(Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(bytes)));
                    return token;
                });
    }

    @Transactional
    public TokenPair rotate(String rawToken) {
        String userKey = getUserKeyFromToken(rawToken);
        if (userKey == null) {
            throw new InvalidRefreshTokenException();
        }

        CompletableFuture<TokenPair> future = new CompletableFuture<>();
        CompletableFuture<TokenPair> existing = inFlightRefreshes.putIfAbsent(userKey, future);
        if (existing != null) {
            return existing.join();
        }

        try {
            TokenPair result = doRotate(rawToken);
            future.complete(result);
            return result;
        } catch (Exception e) {
            future.completeExceptionally(e);
            throw e;
        } finally {
            inFlightRefreshes.remove(userKey, future);
        }
    }

    private String getUserKeyFromToken(String rawToken) {
        try {
            String tokenHash = hash(rawToken);
            return refreshTokenRepository.findByTokenHash(tokenHash)
                    .map(rt -> rt.getUser().getId().toString())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    TokenPair doRotate(String rawToken) {
        String tokenHash = hash(rawToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException());

        if (stored.getRevokedAt() != null) {
            log.warn("Security Warning: Reuse of revoked refresh token detected for user ID: {}! Revoking all active tokens for this user.", stored.getUser().getId());
            refreshTokenRepository.revokeAllByUser(stored.getUser(), Instant.now());
            throw new InvalidRefreshTokenException();
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        stored.setRevokedAt(Instant.now());
        // SLIDING WINDOW: extend expiry on each successful rotation
        stored.setExpiresAt(Instant.now().plusMillis(refreshExpirationMillis));
        refreshTokenRepository.save(stored);
        // Preserve device tracking on rotation
        return issue(stored.getUser(), stored.getDeviceId(), stored.getDeviceName());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash(rawToken))
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    /**
     * Revokes every active refresh token for the given user. Called on logout so a
     * session cannot be resumed later, regardless of which token the client presents.
     */
    @Transactional
    public void revokeAllForUser(Long userId) {
        if (userId == null) return;
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
    }

    private String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    public record TokenPair(String accessToken, String refreshToken) { }
    public static class InvalidRefreshTokenException extends RuntimeException { }
}
