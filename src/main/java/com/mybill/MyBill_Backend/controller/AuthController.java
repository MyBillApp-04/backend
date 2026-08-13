package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.AuthProvider;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.dto.FirebaseLoginRequest;
import com.mybill.MyBill_Backend.dto.RefreshTokenRequest;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import com.mybill.MyBill_Backend.security.JwtTokenDenylist;
import com.mybill.MyBill_Backend.security.JwtUtil;
import com.mybill.MyBill_Backend.service.AuthService;
import com.mybill.MyBill_Backend.service.RefreshTokenService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final MeterRegistry meterRegistry;
    private final JwtUtil jwtUtil;
    private final JwtTokenDenylist tokenDenylist;
    private final RefreshTokenService refreshTokenService;
    private final com.mybill.MyBill_Backend.util.SecurityUtils securityUtils;

    @PostMapping("/firebase-login")
    public ResponseEntity<?> firebaseLogin(
            @Valid @RequestBody FirebaseLoginRequest body,
            @RequestHeader(name = "X-Auth-Flow", defaultValue = "login") String requestedFlow,
            @RequestHeader(name = "X-Device-Id", required = false) String deviceId,
            @RequestHeader(name = "X-Device-Name", required = false) String deviceName
    ) {
        String flow = "refresh".equalsIgnoreCase(requestedFlow) ? "refresh" : "login";
        String idToken = body.token();

        try {
            FirebaseToken decodedToken = verifyIdToken(idToken);

            String email = decodedToken.getEmail();
            if (email == null || email.isBlank()) {
                recordAuthResult("auth_failure", flow, "missing_email");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Firebase token does not contain an email address"));
            }

            // Correctly determine the sign-in provider from the Firebase token claims
            AuthProvider provider = AuthProvider.GOOGLE; // safe default
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> firebaseClaim =
                        (Map<String, Object>) decodedToken.getClaims().get("firebase");
                if (firebaseClaim != null) {
                    String signInProvider = (String) firebaseClaim.get("sign_in_provider");
                    if ("password".equals(signInProvider)) {
                        provider = AuthProvider.LOCAL;
                    }
                    // "google.com" and anything else stays as GOOGLE
                }
            } catch (Exception e) {
                log.warn("Could not determine sign-in provider, defaulting to GOOGLE: exception={} message={}",
                        e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
            }

            // Get display name from token, fall back to email prefix
            String name = (String) decodedToken.getClaims().getOrDefault("name", "");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            User user = authService.firebaseLoginUser(email, name, provider, Role.OWNER);
            RefreshTokenService.TokenPair tokens = refreshTokenService.issue(user, deviceId, deviceName);
            recordAuthResult("auth_success", flow, "accepted");
            log.info("Successful login via {}", provider);
            return ResponseEntity.ok(Map.of("token", tokens.accessToken(), "refreshToken", tokens.refreshToken()));

        } catch (Exception e) {
            recordAuthResult("auth_failure", flow, "server_error");
            log.error("Firebase login failed: exception={} message={}",
                    e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));

            String msg = e.getMessage();
            if (msg != null && msg.contains("FirebaseApp")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Server auth not configured. Contact admin."));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server-side login failure"));
        }
    }

    private void recordAuthResult(String metric, String flow, String outcome) {
        meterRegistry.counter(metric, "flow", flow, "outcome", outcome).increment();
    }

    protected FirebaseToken verifyIdToken(String idToken) throws Exception {
        // Authentication must always be verified by Firebase Admin. In particular, never
        // trust claims decoded from an unsigned/unverified client token in local or prod.
        return FirebaseAuth.getInstance().verifyIdToken(idToken, true);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            RefreshTokenService.TokenPair tokens = refreshTokenService.rotate(request.refreshToken());
            recordAuthResult("auth_success", "refresh", "accepted");
            return ResponseEntity.ok(Map.of("token", tokens.accessToken(), "refreshToken", tokens.refreshToken()));
        } catch (RefreshTokenService.InvalidRefreshTokenException ex) {
            recordAuthResult("auth_failure", "refresh", "invalid_token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String token = bearerToken(request);
        if (token != null && jwtUtil.validateToken(token)) {
            tokenDenylist.deny(token, jwtUtil.extractExpiration(token));
        }
        // Revoke the presented refresh token (if supplied) and, for the authenticated
        // user, all other active refresh tokens so the session cannot be resumed.
        refreshTokenService.revoke(request.getHeader("X-Refresh-Token"));
        try {
            refreshTokenService.revokeAllForUser(securityUtils.getCurrentUserId());
        } catch (RuntimeException ignored) {
            // No authenticated principal (e.g. already-invalid access token); nothing to revoke.
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    private String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        return header.substring(7);
    }

    // Health check
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("MyBill backend is running");
    }
}
