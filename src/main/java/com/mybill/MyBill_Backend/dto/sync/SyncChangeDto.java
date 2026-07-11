package com.mybill.MyBill_Backend.dto.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class SyncChangeDto {
    @NotBlank(message = "Change ID is required")
    @Size(max = 80, message = "Change ID must be 80 characters or fewer")
    private String changeId;

    @NotBlank(message = "Entity type is required")
    @Size(max = 40, message = "Entity type must be 40 characters or fewer")
    private String entityType;

    @NotBlank(message = "Entity ID is required")
    @Size(max = 80, message = "Entity ID must be 80 characters or fewer")
    private String entityId;

    @NotBlank(message = "Sync operation is required")
    @Pattern(regexp = "(?i)^(CREATE|UPDATE|DELETE|UPSERT)$", message = "Sync operation must be CREATE, UPDATE, DELETE, or UPSERT")
    private String operation;

    @NotNull(message = "Sync payload is required")
    private Map<String, Object> payload;

    @NotNull(message = "Change creation time is required")
    private LocalDateTime createdAt;
}
