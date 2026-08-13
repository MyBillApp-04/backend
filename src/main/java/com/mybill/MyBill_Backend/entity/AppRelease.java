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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getVersionCode() { return versionCode; }
    public void setVersionCode(int versionCode) { this.versionCode = versionCode; }

    public String getVersionName() { return versionName; }
    public void setVersionName(String versionName) { this.versionName = versionName; }

    public int getMinimumSupportedVersionCode() { return minimumSupportedVersionCode; }
    public void setMinimumSupportedVersionCode(int minimumSupportedVersionCode) { this.minimumSupportedVersionCode = minimumSupportedVersionCode; }

    public String getApkUrlPrimary() { return apkUrlPrimary; }
    public void setApkUrlPrimary(String apkUrlPrimary) { this.apkUrlPrimary = apkUrlPrimary; }

    public String getApkUrlFallback() { return apkUrlFallback; }
    public void setApkUrlFallback(String apkUrlFallback) { this.apkUrlFallback = apkUrlFallback; }

    public String getApkUrl() { return apkUrl; }
    public void setApkUrl(String apkUrl) { this.apkUrl = apkUrl; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public boolean isForceUpdate() { return forceUpdate; }
    public void setForceUpdate(boolean forceUpdate) { this.forceUpdate = forceUpdate; }

    public AppUpdateType getUpdateType() { return updateType; }
    public void setUpdateType(AppUpdateType updateType) { this.updateType = updateType; }

    public int getRemindAfterDays() { return remindAfterDays; }
    public void setRemindAfterDays(int remindAfterDays) { this.remindAfterDays = remindAfterDays; }

    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static AppReleaseBuilder builder() { return new AppReleaseBuilder(); }

    public static class AppReleaseBuilder {
        private Long id;
        private int versionCode;
        private String versionName;
        private int minimumSupportedVersionCode;
        private String apkUrlPrimary;
        private String apkUrlFallback;
        private String apkUrl;
        private String sha256;
        private boolean forceUpdate;
        private AppUpdateType updateType;
        private int remindAfterDays = 7;
        private String releaseNotes;
        private boolean active = true;
        private OffsetDateTime publishedAt;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public AppReleaseBuilder id(Long id) { this.id = id; return this; }
        public AppReleaseBuilder versionCode(int versionCode) { this.versionCode = versionCode; return this; }
        public AppReleaseBuilder versionName(String versionName) { this.versionName = versionName; return this; }
        public AppReleaseBuilder minimumSupportedVersionCode(int minimumSupportedVersionCode) { this.minimumSupportedVersionCode = minimumSupportedVersionCode; return this; }
        public AppReleaseBuilder apkUrlPrimary(String apkUrlPrimary) { this.apkUrlPrimary = apkUrlPrimary; return this; }
        public AppReleaseBuilder apkUrlFallback(String apkUrlFallback) { this.apkUrlFallback = apkUrlFallback; return this; }
        public AppReleaseBuilder apkUrl(String apkUrl) { this.apkUrl = apkUrl; return this; }
        public AppReleaseBuilder sha256(String sha256) { this.sha256 = sha256; return this; }
        public AppReleaseBuilder forceUpdate(boolean forceUpdate) { this.forceUpdate = forceUpdate; return this; }
        public AppReleaseBuilder updateType(AppUpdateType updateType) { this.updateType = updateType; return this; }
        public AppReleaseBuilder remindAfterDays(int remindAfterDays) { this.remindAfterDays = remindAfterDays; return this; }
        public AppReleaseBuilder releaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; return this; }
        public AppReleaseBuilder active(boolean active) { this.active = active; return this; }
        public AppReleaseBuilder publishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; return this; }
        public AppReleaseBuilder createdAt(OffsetDateTime createdAt) { this.createdAt = createdAt; return this; }
        public AppReleaseBuilder updatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public AppRelease build() {
            AppRelease r = new AppRelease();
            r.id = this.id;
            r.versionCode = this.versionCode;
            r.versionName = this.versionName;
            r.minimumSupportedVersionCode = this.minimumSupportedVersionCode;
            r.apkUrlPrimary = this.apkUrlPrimary;
            r.apkUrlFallback = this.apkUrlFallback;
            r.apkUrl = this.apkUrl;
            r.sha256 = this.sha256;
            r.forceUpdate = this.forceUpdate;
            r.updateType = this.updateType;
            r.remindAfterDays = this.remindAfterDays;
            r.releaseNotes = this.releaseNotes;
            r.active = this.active;
            r.publishedAt = this.publishedAt;
            r.createdAt = this.createdAt;
            r.updatedAt = this.updatedAt;
            return r;
        }
    }
}
