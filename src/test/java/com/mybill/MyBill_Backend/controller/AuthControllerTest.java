package com.mybill.MyBill_Backend.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.mybill.MyBill_Backend.entity.AuthProvider;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.service.AuthService;
import com.mybill.MyBill_Backend.service.RefreshTokenService;
import com.mybill.MyBill_Backend.security.RateLimitFilter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private MeterRegistry meterRegistry;

    private MockedStatic<FirebaseAuth> mockedFirebaseAuth;
    private FirebaseAuth firebaseAuth;
    private FirebaseToken firebaseToken;

    @BeforeEach
    void setUp() throws Exception {
        // Bypass and clear rate limit state to prevent 429 Too Many Requests in tests
        ReflectionTestUtils.setField(rateLimitFilter, "authLimitPerMinute", 100);
        ReflectionTestUtils.setField(rateLimitFilter, "ipLimitPerMinute", 100);
        com.github.benmanes.caffeine.cache.Cache<?, ?> cache =
                (com.github.benmanes.caffeine.cache.Cache<?, ?>) ReflectionTestUtils.getField(rateLimitFilter, "counters");
        if (cache != null) {
            cache.invalidateAll();
        }

        firebaseAuth = mock(FirebaseAuth.class);
        firebaseToken = mock(FirebaseToken.class);

        mockedFirebaseAuth = mockStatic(FirebaseAuth.class);
        mockedFirebaseAuth.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

        when(firebaseAuth.verifyIdToken(anyString(), eq(true))).thenReturn(firebaseToken);
    }

    @AfterEach
    void tearDown() {
        mockedFirebaseAuth.close();
    }

    @Test
    void testGoogleLoginSuccess() throws Exception {
        String testEmail = "googleuser@example.com";
        String testName = "Google User";
        String testJwt = "mock-backend-jwt-token";

        when(firebaseToken.getEmail()).thenReturn(testEmail);

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", testName);
        Map<String, Object> firebaseClaim = new HashMap<>();
        firebaseClaim.put("sign_in_provider", "google.com");
        claims.put("firebase", firebaseClaim);

        when(firebaseToken.getClaims()).thenReturn(claims);
        User user = User.builder().email(testEmail).role(Role.OWNER).build();
        when(authService.firebaseLoginUser(eq(testEmail), eq(testName), eq(AuthProvider.GOOGLE), eq(Role.OWNER)))
                .thenReturn(user);
        when(refreshTokenService.issue(eq(user), any(), any())).thenReturn(new RefreshTokenService.TokenPair(testJwt, "refresh-token"));
        double before = counterValue("auth_success", "refresh", "accepted");

        mockMvc.perform(post("/api/auth/firebase-login")
                        .header("X-Auth-Flow", "refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"valid-google-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(testJwt))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));

        org.assertj.core.api.Assertions.assertThat(
                counterValue("auth_success", "refresh", "accepted") - before
        ).isEqualTo(1.0);
        verify(authService).firebaseLoginUser(eq(testEmail), eq(testName), eq(AuthProvider.GOOGLE), eq(Role.OWNER));
    }

    @Test
    void testEmailLoginSuccess() throws Exception {
        String testEmail = "localuser@example.com";
        String testName = "Local User";
        String testJwt = "mock-backend-jwt-token";

        when(firebaseToken.getEmail()).thenReturn(testEmail);

        Map<String, Object> claims = new HashMap<>();
        claims.put("name", testName);
        Map<String, Object> firebaseClaim = new HashMap<>();
        firebaseClaim.put("sign_in_provider", "password");
        claims.put("firebase", firebaseClaim);

        when(firebaseToken.getClaims()).thenReturn(claims);
        User user = User.builder().email(testEmail).role(Role.OWNER).build();
        when(authService.firebaseLoginUser(eq(testEmail), eq(testName), eq(AuthProvider.LOCAL), eq(Role.OWNER)))
                .thenReturn(user);
        when(refreshTokenService.issue(eq(user), any(), any())).thenReturn(new RefreshTokenService.TokenPair(testJwt, "refresh-token"));

        mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"valid-email-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(testJwt));

        verify(authService).firebaseLoginUser(eq(testEmail), eq(testName), eq(AuthProvider.LOCAL), eq(Role.OWNER));
    }

    @Test
    void testLoginFailureMissingToken() throws Exception {
        mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLoginFailureInvalidToken() throws Exception {
        when(firebaseAuth.verifyIdToken("invalid-token", true)).thenThrow(new RuntimeException("Invalid token"));
        double before = counterValue("auth_failure", "login", "server_error");

        mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-token\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Server-side login failure"));

        org.assertj.core.api.Assertions.assertThat(
                counterValue("auth_failure", "login", "server_error") - before
        ).isEqualTo(1.0);
    }

    @Test
    void testLoginRejectsWhenFirebaseAdminIsNotConfigured() throws Exception {
        mockedFirebaseAuth.when(FirebaseAuth::getInstance)
                .thenThrow(new IllegalStateException("FirebaseApp with name [DEFAULT] doesn't exist."));

        mockMvc.perform(post("/api/auth/firebase-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"unverified-client-token\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Server auth not configured. Contact admin."));

        verifyNoInteractions(authService);
    }

    private double counterValue(String name, String flow, String outcome) {
        var counter = meterRegistry.find(name)
                .tags("flow", flow, "outcome", outcome)
                .counter();
        return counter == null ? 0.0 : counter.count();
    }
}
