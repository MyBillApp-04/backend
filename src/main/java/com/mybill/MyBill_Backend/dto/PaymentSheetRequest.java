package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentSheetRequest {
    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    @NotBlank(message = "Invoice number is required")
    private String invoiceNumber;

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private Double amount;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;

    @NotBlank(message = "Client name is required")
    private String clientName;
}
