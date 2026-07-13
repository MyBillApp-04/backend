package com.mybill.MyBill_Backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(name = "ValidationErrorResponse", description = "Validation error response with field-specific messages.")
public record ValidationErrorResponse(
        @Schema(example = "2026-07-14T10:15:30")
        LocalDateTime timestamp,
        @Schema(example = "400")
        int status,
        @Schema(example = "Bad Request")
        String error,
        @Schema(example = "Validation failed")
        String message,
        @Schema(example = "/api/invoice/generate")
        String path,
        @Schema(example = "8d3f67f7b0f64f6d")
        String requestId,
        @Schema(
                description = "Map of request field names to validation messages.",
                example = "{\"clientId\":\"Client is required\",\"workIds\":\"Select at least one work item\"}"
        )
        Map<String, String> fieldErrors
) {
}
