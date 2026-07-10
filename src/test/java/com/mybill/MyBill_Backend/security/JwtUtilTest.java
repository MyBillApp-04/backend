package com.mybill.MyBill_Backend.security;

import com.mybill.MyBill_Backend.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(
                jwtUtil,
                "secret",
                "unit-test-only-signing-secret-with-high-entropy-9f2a7c4e1b8d"
        );
        ReflectionTestUtils.setField(jwtUtil, "expirationMillis", 3_600_000L);
        jwtUtil.validateSecretKeyConfig();
    }

    @Test
    void signsAndVerifiesToken() {
        String token = jwtUtil.generateToken("owner@example.com", Role.CLIENT);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("owner@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_USER");
        assertThat(jwtUtil.extractAuthorities(token)).containsExactly("ROLE_USER");
        assertThat(jwtUtil.extractScopes(token)).contains("client:read", "client:write", "invoice:read", "invoice:write");
    }

    @Test
    void rejectsTamperedSignature() {
        String token = jwtUtil.generateToken("owner@example.com", Role.CLIENT);
        char replacement = token.endsWith("a") ? 'b' : 'a';
        String tampered = token.substring(0, token.length() - 1) + replacement;

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void usesConfiguredExpiration() {
        long beforeIssuing = System.currentTimeMillis();
        String token = jwtUtil.generateToken("owner@example.com", Role.CLIENT);

        var claims = io.jsonwebtoken.Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) ReflectionTestUtils.invokeMethod(
                        jwtUtil, "getSigningKey"))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getExpiration().getTime())
                .isBetween(beforeIssuing + 3_599_000L, beforeIssuing + 3_601_000L);
    }

    @Test
    void adminTokenCarriesAdminRoleAndScopes() {
        String token = jwtUtil.generateToken("admin@example.com", Role.ADMIN);

        assertThat(jwtUtil.extractRole(token)).isEqualTo("ROLE_ADMIN");
        assertThat(jwtUtil.extractAuthorities(token)).containsExactly("ROLE_ADMIN");
        assertThat(jwtUtil.extractScopes(token)).contains("admin:read", "admin:write");
    }
}
