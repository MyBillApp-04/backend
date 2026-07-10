package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundRequest {
    @NotNull(message = "Refund amount is required")
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Refund amount must use up to 10 digits and 2 decimal places")
    private BigDecimal amount;

    @Size(max = 250, message = "Reason must be 250 characters or fewer")
    private String reason;
}
