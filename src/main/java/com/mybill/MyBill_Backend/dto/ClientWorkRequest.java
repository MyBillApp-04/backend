package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientWorkRequest {
    private UUID id;

    @NotBlank(message = "Description must not be blank")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Rate is required")
    @Min(value = 0, message = "Rate must not be negative")
    private Double rate;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private LocalDateTime date;

    @Size(max = 100, message = "DeviceId must not exceed 100 characters")
    private String deviceId;
}
