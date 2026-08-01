package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.entity.UserDeviceToken;
import com.mybill.MyBill_Backend.repository.UserDeviceTokenRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {

    private final UserDeviceTokenRepository deviceTokenRepository;
    private final SecurityUtils securityUtils;

    public DeviceTokenController(
            UserDeviceTokenRepository deviceTokenRepository,
            SecurityUtils securityUtils) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    public ResponseEntity<?> registerDeviceToken(@RequestBody Map<String, String> payload) {
        User user = securityUtils.getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String fcmToken = payload.get("fcmToken");
        if (fcmToken == null || fcmToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "fcmToken is required"));
        }

        String platform = payload.getOrDefault("platform", "UNKNOWN").toUpperCase();

        Optional<UserDeviceToken> existing = deviceTokenRepository.findByUserIdAndFcmToken(user.getId(), fcmToken.trim());
        UserDeviceToken tokenEntity;
        if (existing.isPresent()) {
            tokenEntity = existing.get();
            tokenEntity.setPlatform(platform);
        } else {
            tokenEntity = UserDeviceToken.builder()
                    .user(user)
                    .fcmToken(fcmToken.trim())
                    .platform(platform)
                    .build();
        }

        deviceTokenRepository.save(tokenEntity);
        return ResponseEntity.ok(Map.of("message", "Device token registered successfully"));
    }

    @DeleteMapping
    public ResponseEntity<?> unregisterDeviceToken(@RequestParam("fcmToken") String fcmToken) {
        Long userId = securityUtils.getCurrentUserId();

        if (fcmToken != null && !fcmToken.isBlank()) {
            deviceTokenRepository.deleteByUserIdAndFcmToken(userId, fcmToken.trim());
        }

        return ResponseEntity.ok(Map.of("message", "Device token unregistered successfully"));
    }
}
