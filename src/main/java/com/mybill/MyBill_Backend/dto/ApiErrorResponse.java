package com.mybill.MyBill_Backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "ApiErrorResponse", description = "Standard MyBill API error response.")
public record ApiErrorResponse(
        @Schema(example = "2026-07-14T10:15:30")
        LocalDateTime timestamp,
        @Schema(example = "404")
        int status,
        @Schema(example = "Not Found")
        String error,
        @Schema(example = "Resource not found")
        String message,
        @Schema(example = "/api/invoice/123e4567-e89b-12d3-a456-426614174000")
        String path,
        @Schema(example = "8d3f67f7b0f64f6d")
        String requestId
) {
}
