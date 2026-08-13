package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemDTO {
    private UUID id;

    @NotBlank(message = "Item description is required")
    @Size(max = 500, message = "Item description must be 500 characters or fewer")
    private String description;

    @Size(max = 200, message = "Dimension must be 200 characters or fewer")
    private String dimension;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @DecimalMin(value = "0.00", message = "KGs cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "KGs can have at most 2 decimal places")
    private Double kgs;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 12, fraction = 2, message = "Amount can have at most 2 decimal places")
    private Double amount;

    private Integer version;
}