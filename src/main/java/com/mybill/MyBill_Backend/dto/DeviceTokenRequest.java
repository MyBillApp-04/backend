package com.mybill.MyBill_Backend.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record DeviceTokenRequest(@NotBlank @Size(max = 4096) String fcmToken,
                                 @Size(max = 20) @Pattern(regexp = "(?i)android|ios|web|unknown") String platform) { }
