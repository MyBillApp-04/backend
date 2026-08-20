package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.FirebaseLoginRequest;
import com.mybill.MyBill_Backend.dto.RefreshTokenRequest;
import com.mybill.MyBill_Backend.entity.AuthProvider;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.entity.RefreshToken;
import com.mybill.MyBill_Backend.observability.SecureLogMessageConverter;
import com.mybill.MyBill_Backend.security.JwtTokenDenylist;
import com.mybill.MyBill_Backend.security.JwtUtil;
import com.mybill.MyBill_Backend.service.AuthService;
import com.mybill.MyBill_Backend.service.OtpService;
import com.mybill.MyBill_Backend.service.RefreshTokenService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import com.mybill.MyBill_Backend.repository.RefreshTokenRepository;
import com.mybill.MyBill_Backend.repository.UserRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;

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
    private final OtpService otpService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
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

            // Check RefreshToken for (userId, deviceId) to determine auth state.
            // Token rotation creates multiple rows per device, so always pick the
            // most recent trusted one instead of querying for a single unique row.
            String resolvedDeviceId = deviceId != null ? deviceId : "unknown";
            String resolvedDeviceName = deviceName != null ? deviceName : "unknown";

            Optional<RefreshToken> existingTokenOpt =
                    refreshTokenRepository.findFirstByUserIdAndDeviceIdAndTrustedTrueOrderByCreatedAtDesc(
                            user.getId(), resolvedDeviceId);

            if (existingTokenOpt.isEmpty()) {
                // OTP device verification is temporarily disabled. Any device is
                // auto-trusted so login proceeds straight to the MPIN step.
                refreshTokenService.trustDevice(user, resolvedDeviceId, resolvedDeviceName);
                log.info("OTP bypass: device {} auto-trusted for user {}", resolvedDeviceId, email);
            }

            // OTP and MPIN verification are temporarily bypassed. Login always
            // issues the real session tokens and returns SUCCESS immediately.
            RefreshTokenService.TokenPair pair =
                    refreshTokenService.issueTrusted(user, resolvedDeviceId, resolvedDeviceName);
            recordAuthResult("auth_success", flow, "success");
            log.info("Login success (OTP/MPIN bypassed) for user {}", email);
            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "token", pair.accessToken(),
                    "refreshToken", pair.refreshToken()
            ));

        } catch (FirebaseAuthException e) {
            // Specific handling for Firebase token verification failure
            recordAuthResult("auth_failure", flow, "firebase_token_verification_failed");
            log.error("Firebase token verification failed: exception={} message={}",
                    e.getClass().getSimpleName(), SecureLogMessageConverter.sanitize(e.getMessage()));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Firebase token verification failed. Please check Firebase configuration and try again."));
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

    @PostMapping("/verify-device-otp")
    public ResponseEntity<?> verifyDeviceOtp(@RequestBody Map<String, String> payload,
                                            @RequestHeader(name = "X-Device-Id") String deviceId) {
        String email = payload.get("email");
        String inputOtp = payload.get("otp");

        if (email == null || inputOtp == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and OTP are required"));
        }

        boolean valid = otpService.verifyOtp(email, inputOtp);

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid or expired OTP"));
        }

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No account found with this email"));
        }

        // OTP verified → trust this device so subsequent logins skip OTP.
        refreshTokenService.trustDevice(user, deviceId, "unknown");

        String status = user.isMpinSet() ? "ENTER_MPIN_REQUIRED" : "SETUP_MPIN_REQUIRED";
        log.info("Device {} trusted for user {} - next step {}", deviceId, email, status);

        return ResponseEntity.ok(Map.of(
                "status", status,
                "email", email
        ));
    }

    @PostMapping("/setup-mpin")
    public ResponseEntity<?> setupMpin(@RequestBody Map<String, String> payload,
                                       @RequestHeader(name = "X-Device-Id") String deviceId) {
        String email = payload.get("email");
        String mpin = payload.get("mpin");

        if (email == null || mpin == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and 4-digit MPIN are required"));
        }

        if (!mpin.matches("\\d{4}")) {
            return ResponseEntity.badRequest().body(Map.of("error", "MPIN must be exactly 4 digits"));
        }

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No account found with this email"));
        }

        user.setMpinHash(BCrypt.hashpw(mpin, BCrypt.gensalt()));
        user.setMpinSet(true);
        user.setMpinAttempts(0);
        userRepository.save(user);

        log.info("MPIN set for user {}", email);

        return ResponseEntity.ok(Map.of(
                "status", "ENTER_MPIN_REQUIRED",
                "email", email
        ));
    }

    @PostMapping("/verify-mpin")
    public ResponseEntity<?> verifyMpin(@RequestBody Map<String, String> payload,
                                        @RequestHeader(name = "X-Device-Id") String deviceId,
                                        @RequestHeader(name = "X-Device-Name") String deviceName) {
        String email = payload.get("email");
        String inputMpin = payload.get("mpin");

        if (email == null || inputMpin == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and MPIN are required"));
        }

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No account found with this email"));
        }

        if (!user.isMpinSet() || user.getMpinHash() == null || user.getMpinHash().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "MPIN has not been set for this account"));
        }

        if (!BCrypt.checkpw(inputMpin, user.getMpinHash())) {
            int attempts = user.getMpinAttempts() + 1;
            user.setMpinAttempts(attempts);
            userRepository.save(user);

            int remaining = Math.max(0, 5 - attempts);
            log.warn("Invalid MPIN for user {} - attempt {}/5", email, attempts);

            if (remaining <= 0) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("error", "Too many incorrect attempts. Please sign in again."));
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Incorrect MPIN. " + remaining + " attempt(s) remaining."));
        }

        // Success: reset attempt counter and issue the real session tokens.
        user.setMpinAttempts(0);
        userRepository.save(user);

        String resolvedDeviceName = deviceName != null && !deviceName.isBlank() ? deviceName : "unknown";
        RefreshTokenService.TokenPair pair =
                refreshTokenService.issueTrusted(user, deviceId, resolvedDeviceName);

        log.info("MPIN verified for user {}", email);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "token", pair.accessToken(),
                "refreshToken", pair.refreshToken()
        ));
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request,
            @RequestHeader(name = "X-Refresh-Token", required = false) String rawRefreshToken) {
        String bearer = request.getHeader("Authorization");

        // Denylist the current access token so it can no longer be used.
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            try {
                Date expiresAt = jwtUtil.extractExpiration(token);
                tokenDenylist.deny(token, expiresAt);
            } catch (Exception e) {
                log.info("Logout: could not parse/deny access token: message={}",
                        SecureLogMessageConverter.sanitize(e.getMessage()));
            }
        }

        // Revoke the presented refresh token, terminating the session on this device.
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }

        recordAuthResult("auth_logout", "logout", "success");
        return ResponseEntity.noContent().build();
    }

    private void recordAuthResult(String metric, String flow, String outcome) {
        meterRegistry.counter(metric, "flow", flow, "outcome", outcome).increment();
    }

    protected FirebaseToken verifyIdToken(String idToken) throws Exception {
        // Authentication must always be verified by Firebase Admin. In particular, never
        // trust claims decoded from an unsigned/unverified client token in local or prod.
        return FirebaseAuth.getInstance().verifyIdToken(idToken, false);
    }
}