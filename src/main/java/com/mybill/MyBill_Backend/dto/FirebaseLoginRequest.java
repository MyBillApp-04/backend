package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FirebaseLoginRequest(
        @NotBlank(message = "Firebase token is required")
        @Size(max = 16_384, message = "Firebase token is too large")
        String token
) { }
