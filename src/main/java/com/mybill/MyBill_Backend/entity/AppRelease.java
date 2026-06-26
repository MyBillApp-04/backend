package com.mybill.MyBill_Backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/** Immutable APK release metadata. APK bytes are hosted outside Render. */
@Entity
@Table(name = "app_release", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppRelease {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_code", nullable = false, unique = true)
    private int versionCode;

    @Column(name = "version_name", nullable = false, length = 40)
    private String versionName;

    @Column(name = "minimum_supported_version_code", nullable = false)
    private int minimumSupportedVersionCode;

    @Column(name = "apk_url_primary", nullable = false, length = 2048)
    private String apkUrlPrimary;

    @Column(name = "apk_url_fallback", length = 2048)
    private String apkUrlFallback;

    /**
     * Legacy column retained by migration compatibility only. New update logic
     * uses apkUrlPrimary/apkUrlFallback.
     */
    @Column(name = "apk_url", nullable = false, length = 2048)
    private String apkUrl;

    /** Lowercase SHA-256 of the exact signed APK; prevents a modified download installing. */
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    /** Legacy column retained by migration compatibility only. */
    @Column(name = "force_update", nullable = false)
    private boolean forceUpdate;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_type", nullable = false, length = 10)
    private AppUpdateType updateType;

    @Column(name = "remind_after_days", nullable = false)
    private int remindAfterDays;

    @Column(name = "release_notes", nullable = false, length = 2000)
    private String releaseNotes;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (publishedAt == null) publishedAt = now;
        if (createdAt == null) createdAt = now;
        updatedAt = now;
        if (apkUrl == null && apkUrlPrimary != null) apkUrl = apkUrlPrimary;
        forceUpdate = updateType == AppUpdateType.FORCE;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (apkUrl == null && apkUrlPrimary != null) apkUrl = apkUrlPrimary;
        forceUpdate = updateType == AppUpdateType.FORCE;
    }
}
