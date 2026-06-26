package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.AppReleaseRequest;
import com.mybill.MyBill_Backend.dto.AppVersionResponse;
import com.mybill.MyBill_Backend.entity.AppRelease;
import com.mybill.MyBill_Backend.entity.AppUpdateType;
import com.mybill.MyBill_Backend.entity.Role;
import com.mybill.MyBill_Backend.entity.User;
import com.mybill.MyBill_Backend.repository.AppReleaseRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AppReleaseServiceTest {

    private static final String SHA256 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void currentVersionBelowMinimumForcesUpdate() {
        AppReleaseService service = service(repositoryWith(activeRelease(10, 5)), adminSecurity());

        AppVersionResponse response = service.checkVersion(4, "1.0.4", "android").orElseThrow();

        assertThat(response.getUpdateType()).isEqualTo("FORCE");
        assertThat(response.isForceUpdate()).isTrue();
        assertThat(response.isSoftUpdate()).isFalse();
        assertThat(response.isUpdateAvailable()).isTrue();
    }

    @Test
    void currentVersionBelowLatestButSupportedGetsSoftUpdate() {
        AppReleaseService service = service(repositoryWith(activeRelease(10, 5)), adminSecurity());

        AppVersionResponse response = service.checkVersion(7, "1.0.7", "android").orElseThrow();

        assertThat(response.getUpdateType()).isEqualTo("SOFT");
        assertThat(response.isForceUpdate()).isFalse();
        assertThat(response.isSoftUpdate()).isTrue();
        assertThat(response.isUpdateAvailable()).isTrue();
    }

    @Test
    void currentVersionAtLatestGetsNoUpdate() {
        AppReleaseService service = service(repositoryWith(activeRelease(10, 5)), adminSecurity());

        AppVersionResponse response = service.checkVersion(10, "1.0.10", "android").orElseThrow();

        assertThat(response.getUpdateType()).isEqualTo("NONE");
        assertThat(response.isUpdateAvailable()).isFalse();
        assertThat(response.getUserMessage()).isEqualTo("MyBill is up to date.");
    }

    @Test
    void invalidApkUrlIsRejected() {
        AppReleaseService service = service(repositoryWith(Optional.empty()), adminSecurity());

        assertThatThrownBy(() -> service.createAndPublish(request("http://example.com/app.apk", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("apkUrlPrimary must use HTTPS");
    }

    @Test
    void onlyAdminCanPublishRelease() {
        AppReleaseService service = service(repositoryWith(Optional.empty()), clientSecurity());

        assertThatThrownBy(() -> service.createAndPublish(request("https://example.com/app.apk", null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Administrator access required");
    }

    @Test
    void publishingDeactivatesPreviousActiveRelease() {
        AppRelease active = activeRelease(10, 5);
        AppReleaseRepository repository = repositoryWith(Optional.of(active));
        when(repository.save(any(AppRelease.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppReleaseService service = service(repository, adminSecurity());

        AppRelease saved = service.createAndPublish(new AppReleaseRequest(
                11,
                "1.0.11",
                5,
                "https://example.com/app-11.apk",
                "https://backup.example.com/app-11.apk",
                SHA256,
                "Simple billing improvements.",
                AppUpdateType.SOFT,
                null,
                true));

        assertThat(active.isActive()).isFalse();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getApkUrlPrimary()).isEqualTo("https://example.com/app-11.apk");
        assertThat(saved.getApkUrlFallback()).isEqualTo("https://backup.example.com/app-11.apk");
        assertThat(saved.getRemindAfterDays()).isEqualTo(3);
        verify(repository, atLeastOnce()).save(any(AppRelease.class));
    }

    private AppReleaseService service(AppReleaseRepository repository, SecurityUtils securityUtils) {
        return new AppReleaseService(repository, securityUtils);
    }

    private AppReleaseRepository repositoryWith(AppRelease release) {
        return repositoryWith(Optional.of(release));
    }

    private AppReleaseRepository repositoryWith(Optional<AppRelease> release) {
        AppReleaseRepository repository = mock(AppReleaseRepository.class);
        when(repository.findFirstByActiveTrueOrderByVersionCodeDesc()).thenReturn(release);
        when(repository.existsByVersionCode(anyInt())).thenReturn(false);
        when(repository.save(any(AppRelease.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return repository;
    }

    private AppRelease activeRelease(int versionCode, int minimumSupportedVersionCode) {
        return AppRelease.builder()
                .versionCode(versionCode)
                .versionName("1.0." + versionCode)
                .minimumSupportedVersionCode(minimumSupportedVersionCode)
                .apkUrlPrimary("https://example.com/mybill.apk")
                .apkUrl("https://example.com/mybill.apk")
                .sha256(SHA256)
                .forceUpdate(false)
                .updateType(AppUpdateType.SOFT)
                .remindAfterDays(3)
                .releaseNotes("Simple billing improvements.")
                .active(true)
                .publishedAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private AppReleaseRequest request(String primaryUrl, String fallbackUrl) {
        return new AppReleaseRequest(
                20,
                "1.2.0",
                10,
                primaryUrl,
                fallbackUrl,
                SHA256,
                "Simple billing improvements.",
                AppUpdateType.SOFT,
                3,
                true);
    }

    private SecurityUtils adminSecurity() {
        return security(Role.ADMIN);
    }

    private SecurityUtils clientSecurity() {
        return security(Role.CLIENT);
    }

    private SecurityUtils security(Role role) {
        SecurityUtils securityUtils = mock(SecurityUtils.class);
        when(securityUtils.getCurrentUser()).thenReturn(User.builder().role(role).build());
        return securityUtils;
    }
}
