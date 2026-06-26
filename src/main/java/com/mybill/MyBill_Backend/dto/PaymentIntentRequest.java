package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentIntentRequest {
    @NotNull(message = "Invoice ID is required")
    private UUID invoiceId;

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private Double amount;

    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;
}
