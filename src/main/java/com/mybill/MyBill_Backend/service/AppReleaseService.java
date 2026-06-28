package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.AppReleaseRequest;
import com.mybill.MyBill_Backend.dto.AppVersionResponse;
import com.mybill.MyBill_Backend.entity.AppRelease;
import com.mybill.MyBill_Backend.entity.AppUpdateType;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.repository.AppReleaseRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppReleaseService {
    private final AppReleaseRepository repository;
    private final SecurityUtils securityUtils;
    private static final String SOFT_MESSAGE = "A new MyBill update is available. You can update now or later.";
    private static final String FORCE_MESSAGE = "MyBill needs an important update to keep your billing data safe. Please update now to continue.";
    private static final String NONE_MESSAGE = "MyBill is up to date.";

    @Transactional(readOnly = true)
    public Optional<AppVersionResponse> latest() {
        return checkVersion(null, null, "android");
    }

    @Transactional(readOnly = true)
    public Optional<AppVersionResponse> checkVersion(Integer currentVersionCode, String currentVersionName, String platform) {
        if (platform != null && !platform.isBlank() && !"android".equalsIgnoreCase(platform.trim())) {
            return repository.findFirstByActiveTrueOrderByVersionCodeDesc()
                    .map(release -> toResponse(release, currentVersionCode, currentVersionName, AppUpdateType.NONE));
        }
        return repository.findFirstByActiveTrueOrderByVersionCodeDesc()
                .map(release -> toResponse(release, currentVersionCode, currentVersionName,
                        decideUpdateType(release, currentVersionCode)));
    }

    @Transactional(readOnly = true)
    public List<AppRelease> all() {
        requireAdmin();
        return repository.findAllByOrderByVersionCodeDesc();
    }

    public AppRelease createAndPublish(AppReleaseRequest request) {
        requireAdmin();
        validateRequest(request);
        if (repository.existsByVersionCode(request.versionCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "versionCode already exists");
        }
        repository.findFirstByActiveTrueOrderByVersionCodeDesc().ifPresent(active -> {
            if (active.getVersionCode() >= request.versionCode()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "versionCode must be greater than the currently published release");
            }
            active.setActive(false);
            repository.saveAndFlush(active);
        });

        return repository.save(AppRelease.builder()
                .versionCode(request.versionCode())
                .versionName(request.versionName().trim())
                .minimumSupportedVersionCode(request.minimumSupportedVersionCode())
                .apkUrlPrimary(request.apkUrlPrimary().trim())
                .apkUrlFallback(normalizeOptional(request.apkUrlFallback()))
                .apkUrl(request.apkUrlPrimary().trim())
                .sha256(request.sha256().toLowerCase(Locale.ROOT))
                .forceUpdate(request.updateType() == AppUpdateType.FORCE)
                .updateType(request.updateType())
                .remindAfterDays(remindAfterDays(request.updateType(), request.remindAfterDays()))
                .releaseNotes(request.releaseNotes().trim())
                .active(true)
                .publishedAt(OffsetDateTime.now())
                .build());
    }

    AppUpdateType decideUpdateType(AppRelease release, Integer currentVersionCode) {
        if (currentVersionCode == null || currentVersionCode <= 0) {
            return release.getVersionCode() > 0 ? release.getUpdateType() : AppUpdateType.NONE;
        }
        if (currentVersionCode < release.getMinimumSupportedVersionCode()) {
            return AppUpdateType.FORCE;
        }
        if (currentVersionCode < release.getVersionCode()) {
            return AppUpdateType.SOFT;
        }
        return AppUpdateType.NONE;
    }

    private AppVersionResponse toResponse(
            AppRelease release,
            Integer currentVersionCode,
            String currentVersionName,
            AppUpdateType decision
    ) {
        boolean forceUpdate = decision == AppUpdateType.FORCE;
        boolean softUpdate = decision == AppUpdateType.SOFT;
        String userMessage = switch (decision) {
            case FORCE -> FORCE_MESSAGE;
            case SOFT -> SOFT_MESSAGE;
            case NONE -> NONE_MESSAGE;
        };

        return new AppVersionResponse(
                currentVersionCode,
                normalizeOptional(currentVersionName),
                release.getVersionCode(),
                release.getVersionName(),
                release.getMinimumSupportedVersionCode(),
                forceUpdate || softUpdate,
                decision.name(),
                forceUpdate,
                softUpdate,
                release.getApkUrlPrimary(),
                release.getApkUrlFallback(),
                release.getSha256(),
                release.getReleaseNotes(),
                release.getRemindAfterDays(),
                userMessage);
    }

    private void validateRequest(AppReleaseRequest request) {
        if (request.minimumSupportedVersionCode() > request.versionCode()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "minimumSupportedVersionCode cannot be greater than versionCode");
        }
        if (request.releaseNotes().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "releaseNotes must not be blank");
        }
        if (!isHttps(request.apkUrlPrimary())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "apkUrlPrimary must use HTTPS");
        }
        String fallback = normalizeOptional(request.apkUrlFallback());
        if (fallback != null && !isHttps(fallback)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "apkUrlFallback must use HTTPS");
        }
    }

    private int remindAfterDays(AppUpdateType updateType, Integer requestedDays) {
        if (requestedDays != null) return requestedDays;
        return updateType == AppUpdateType.FORCE ? 0 : 3;
    }

    private boolean isHttps(String value) {
        return value != null && value.trim().startsWith("https://");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private void requireAdmin() {
        if (securityUtils.getCurrentUser().getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        }
    }
}
