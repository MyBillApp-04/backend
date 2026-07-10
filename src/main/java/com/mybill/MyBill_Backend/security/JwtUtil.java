package com.mybill.MyBill_Backend.security;

import com.mybill.MyBill_Backend.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMillis;

    @PostConstruct
    public void validateSecretKeyConfig() {
        JwtSecretValidator.validate(secret);
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email, Role role) {
        String authority = authorityFor(role);
        List<String> scopes = scopesFor(role);

        return Jwts.builder()
                .subject(email)
                .claim("role", authority)
                .claim("authorities", List.of(authority))
                .claim("scope", String.join(" ", scopes))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateToken(String email) {
        return generateToken(email, Role.CLIENT);
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        Object authorities = getClaims(token).get("authorities");
        if (authorities instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }

        String role = extractRole(token);
        return role == null || role.isBlank() ? List.of() : List.of(role);
    }

    public List<String> extractScopes(String token) {
        String scope = getClaims(token).get("scope", String.class);
        if (scope == null || scope.isBlank()) {
            return List.of();
        }

        return List.of(scope.split("\\s+"));
    }

    public static String authorityFor(Role role) {
        if (role == Role.ADMIN) {
            return "ROLE_ADMIN";
        }
        return "ROLE_USER";
    }

    private List<String> scopesFor(Role role) {
        if (role == Role.ADMIN) {
            return List.of(
                    "client:read",
                    "client:write",
                    "invoice:read",
                    "invoice:write",
                    "admin:read",
                    "admin:write"
            );
        }

        return List.of(
                "client:read",
                "client:write",
                "invoice:read",
                "invoice:write"
        );
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
