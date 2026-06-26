package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.AppVersionResponse;
import com.mybill.MyBill_Backend.dto.AppReleaseRequest;
import com.mybill.MyBill_Backend.entity.AppRelease;
import com.mybill.MyBill_Backend.service.AppReleaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint – returns the current latest app version metadata.
 * No authentication required so the client can check for updates
 * even before the user logs in (or when the token has expired).
 *
 * Release records live in PostgreSQL so publishing an APK does not require a
 * Render redeploy. APK URLs must be immutable HTTPS asset URLs.
 */
@RestController
@RequiredArgsConstructor
public class AppVersionController {
    private final AppReleaseService appReleaseService;

    @GetMapping("/api/app-version")
    public ResponseEntity<AppVersionResponse> getAppVersion(
            @RequestParam(required = false) Integer currentVersionCode,
            @RequestParam(required = false) String currentVersionName,
            @RequestParam(required = false, defaultValue = "android") String platform
    ) {
        return appReleaseService.checkVersion(currentVersionCode, currentVersionName, platform).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/api/admin/app-releases")
    public ResponseEntity<java.util.List<AppRelease>> allReleases() {
        return ResponseEntity.ok(appReleaseService.all());
    }

    @org.springframework.web.bind.annotation.PostMapping("/api/admin/app-releases")
    public ResponseEntity<AppRelease> publish(@Valid @RequestBody AppReleaseRequest request) {
        return ResponseEntity.status(201).body(appReleaseService.createAndPublish(request));
    }
}
