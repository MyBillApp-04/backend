package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.AuthProvider;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.service.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    @PostMapping("/firebase-login")
    public ResponseEntity<?> firebaseLogin(@RequestBody Map<String, String> body) {
        String idToken = body.get("token");

        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing token in request body"));
        }

        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            String email = decodedToken.getEmail();
            if (email == null || email.isBlank()) {
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
                log.warn("Could not determine sign-in provider, defaulting to GOOGLE: {}", e.getMessage());
            }

            // Get display name from token, fall back to email prefix
            String name = (String) decodedToken.getClaims().getOrDefault("name", "");
            if (name == null || name.isBlank()) {
                name = email.split("@")[0];
            }

            String jwt = authService.firebaseLogin(email, name, provider, Role.CLIENT);
            log.info("Successful login for: {} via {}", email, provider);
            return ResponseEntity.ok(Map.of("token", jwt));

        } catch (Exception e) {
            log.error("Firebase login failed", e);

            String msg = e.getMessage();
            if (msg != null && msg.contains("FirebaseApp")) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Server auth not configured. Contact admin."));
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server-side login failure"));
        }
    }

    // Health check
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("MyBill backend is running");
    }
}