package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.entity.RefreshToken;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.RefreshTokenRepository;
import com.mybill.MyBill_Backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.security.jwt.refresh-expiration:2592000000}")
    private long refreshExpirationMillis;

    @Transactional
    public TokenPair issue(User user) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setCreatedAt(Instant.now());
        refreshToken.setExpiresAt(Instant.now().plusMillis(refreshExpirationMillis));
        refreshTokenRepository.save(refreshToken);
        return new TokenPair(jwtUtil.generateToken(user.getEmail(), user.getRole()), rawToken);
    }

    @Transactional
    public TokenPair rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash(rawToken))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new InvalidRefreshTokenException());
        stored.setRevokedAt(Instant.now());
        return issue(stored.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(hash(rawToken))
                .ifPresent(token -> token.setRevokedAt(Instant.now()));
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
