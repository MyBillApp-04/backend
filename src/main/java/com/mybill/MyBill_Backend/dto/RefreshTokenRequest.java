package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token is required")
        @Size(max = 128, message = "Refresh token is too large")
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Refresh token format is invalid")
        String refreshToken
) { }
