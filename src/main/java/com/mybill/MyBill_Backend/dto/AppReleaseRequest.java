package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.AppUpdateType;
import jakarta.validation.constraints.*;

public record AppReleaseRequest(
        @NotNull @Positive Integer versionCode,
        @NotBlank @Size(max = 40) String versionName,
        @NotNull @Positive Integer minimumSupportedVersionCode,
        @NotBlank @Pattern(regexp = "https://.+", message = "apkUrlPrimary must use HTTPS") @Size(max = 2048) String apkUrlPrimary,
        @Pattern(regexp = "^\\s*$|https://.+", message = "apkUrlFallback must use HTTPS") @Size(max = 2048) String apkUrlFallback,
        @NotBlank @Pattern(regexp = "(?i)[a-f0-9]{64}", message = "sha256 must be a 64 character hex SHA-256") String sha256,
        @NotBlank @Size(max = 2000) String releaseNotes,
        @NotNull AppUpdateType updateType,
        @PositiveOrZero Integer remindAfterDays,
        Boolean active
) {}
