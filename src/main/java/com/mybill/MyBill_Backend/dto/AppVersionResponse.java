package com.mybill.MyBill_Backend.dto;

public class AppVersionResponse {

    private final Integer currentVersionCode;
    private final String currentVersionName;
    private final int latestVersionCode;
    private final String latestVersionName;
    private final int minimumSupportedVersionCode;
    private final boolean updateAvailable;
    private final String updateType;
    private final boolean forceUpdate;
    private final boolean softUpdate;
    private final String apkUrlPrimary;
    private final String apkUrlFallback;
    private final String sha256;
    private final String releaseNotes;
    private final int remindAfterDays;
    private final String userMessage;

    public AppVersionResponse(
            Integer currentVersionCode,
            String currentVersionName,
            int latestVersionCode,
            String latestVersionName,
            int minimumSupportedVersionCode,
            boolean updateAvailable,
            String updateType,
            boolean forceUpdate,
            boolean softUpdate,
            String apkUrlPrimary,
            String apkUrlFallback,
            String sha256,
            String releaseNotes,
            int remindAfterDays,
            String userMessage
    ) {
        this.currentVersionCode = currentVersionCode;
        this.currentVersionName = currentVersionName;
        this.latestVersionCode = latestVersionCode;
        this.latestVersionName = latestVersionName;
        this.minimumSupportedVersionCode = minimumSupportedVersionCode;
        this.updateAvailable = updateAvailable;
        this.updateType = updateType;
        this.forceUpdate = forceUpdate;
        this.softUpdate = softUpdate;
        this.apkUrlPrimary = apkUrlPrimary;
        this.apkUrlFallback = apkUrlFallback;
        this.sha256 = sha256;
        this.releaseNotes = releaseNotes;
        this.remindAfterDays = remindAfterDays;
        this.userMessage = userMessage;
    }

    public Integer getCurrentVersionCode() {
        return currentVersionCode;
    }

    public String getCurrentVersionName() {
        return currentVersionName;
    }

    public int getLatestVersionCode() {
        return latestVersionCode;
    }

    public String getLatestVersionName() {
        return latestVersionName;
    }

    public int getMinimumSupportedVersionCode() {
        return minimumSupportedVersionCode;
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getUpdateType() {
        return updateType;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public boolean isSoftUpdate() {
        return softUpdate;
    }

    public String getApkUrlPrimary() {
        return apkUrlPrimary;
    }

    public String getApkUrlFallback() {
        return apkUrlFallback;
    }

    public String getApkUrl() {
        return apkUrlPrimary;
    }

    public String getSha256() { return sha256; }

    public String getReleaseNotes() {
        return releaseNotes;
    }

    public int getRemindAfterDays() {
        return remindAfterDays;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getMessage() {
        return userMessage;
    }
}
